/*
 * Copyright 2017 eneim@Eneim Labs, nam@ene.im
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.ene.toro;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static im.ene.toro.Toro.getManager;
import static im.ene.toro.Toro.getStrategy;

/**
 * @author eneim.
 * @since 5/12/17.
 */

public class PlayerListView extends RecyclerView {

  public PlayerListView(Context context) {
    super(context);
  }

  public PlayerListView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public PlayerListView(Context context, @Nullable AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  RecyclerListener internalRecyclerListener;

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (internalRecyclerListener == null) {
      internalRecyclerListener = new RecyclerListener() {
        @Override public void onViewRecycled(ViewHolder holder) {
          PlayerListView.this.onViewRecycled(holder);
        }
      };

      super.setRecyclerListener(internalRecyclerListener);
    }
  }

  @Override protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    internalRecyclerListener = null;
  }

  @CallSuper @Override public void setRecyclerListener(final RecyclerListener listener) {
    super.setRecyclerListener(new RecyclerListener() {
      @Override public void onViewRecycled(ViewHolder holder) {
        if (listener != null) {
          listener.onViewRecycled(holder);
        }

        if (internalRecyclerListener != null) {
          internalRecyclerListener.onViewRecycled(holder);
        }
      }
    });
  }

  @Override public void onChildAttachedToWindow(final View child) {
    super.onChildAttachedToWindow(child);
    ViewHolder holder = getChildViewHolder(child);
    if (holder == null || !(holder instanceof ToroPlayer)) {
      return;
    }

    final ToroPlayer player = (ToroPlayer) holder;

    final PlayerManager manager = getManager(this);
    if (manager == null) {
      return;
    }

    if (manager.getPlayer() == player) {
      if (!player.isPrepared()) {
        player.preparePlayer(false);
      } else {
        manager.restorePlaybackState(player.getMediaId());
        manager.startPlayback();
      }
    } else if (manager.getPlayer() == null) {
      child.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
        @Override public void onGlobalLayout() {
          child.getViewTreeObserver().removeOnGlobalLayoutListener(this);
          if (player.wantsToPlay() && getStrategy().allowsToPlay(player, PlayerListView.this)) {
            if (!player.isPrepared()) {
              player.preparePlayer(false);
            } else {
              manager.setPlayer(player);
              manager.restorePlaybackState(player.getMediaId());
              manager.startPlayback();
            }
          }
        }
      });
    }
  }

  // Unused for now
  @Override public void onChildDetachedFromWindow(View child) {
    super.onChildDetachedFromWindow(child);
  }

  void onViewRecycled(ViewHolder holder) {
    if (!(holder instanceof ToroPlayer)) {
      return;
    }

    final ToroPlayer player = (ToroPlayer) holder;

    PlayerManager manager = getManager(this);
    // Manually save Video state
    if (manager != null && player == manager.getPlayer()) {
      if (player.isPlaying()) {
        manager.savePlaybackState(player.getMediaId(), player.getCurrentPosition(),
            player.getDuration());
        manager.pausePlayback();
      }
      // Detach current Player
      manager.setPlayer(null);
    }
    // Release player.
    player.releasePlayer();
  }

  // handle scrolling events

  private final List<ToroPlayer> candidates = new ArrayList<>();

  @Override public void onScrolled(int dx, int dy) {
    super.onScrolled(dx, dy);
  }

  @Override public void onScrollStateChanged(int newState) {
    super.onScrollStateChanged(newState);
    if (newState != RecyclerView.SCROLL_STATE_IDLE) {
      return;
    }

    PlayerManager playerManager = getManager(this);
    if (playerManager == null) {
      return;
    }

    // clear current playback candidates
    candidates.clear();
    // Check current playing position
    final ToroPlayer currentPlayer = playerManager.getPlayer();
    if (currentPlayer != null && currentPlayer.getPlayOrder() != RecyclerView.NO_POSITION) {
      if (currentPlayer.wantsToPlay() && getStrategy().allowsToPlay(currentPlayer, this)) {
        candidates.add(currentPlayer);
      }
    }

    int firstPosition = RecyclerView.NO_POSITION;
    int lastPosition = RecyclerView.NO_POSITION;

    // Find visible positions range
    if (getLayoutManager() instanceof LinearLayoutManager) {
      LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
      firstPosition = layoutManager.findFirstVisibleItemPosition();
      lastPosition = layoutManager.findLastVisibleItemPosition();
    } else if (getLayoutManager() instanceof StaggeredGridLayoutManager) {
      StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) getLayoutManager();

      // StaggeredGridLayoutManager can have many rows or columns ...
      int[] firstVisibleItemPositions = layoutManager.findFirstVisibleItemPositions(null);
      int[] lastVisibleItemPositions = layoutManager.findLastVisibleItemPositions(null);

      // TODO Consider to use Arrays#sort() instead?
      List<Integer> firstVisiblePositions = ToroUtil.asList(firstVisibleItemPositions);
      List<Integer> lastVisiblePositions = ToroUtil.asList(lastVisibleItemPositions);

      firstPosition = Collections.min(firstVisiblePositions);
      lastPosition = Collections.max(lastVisiblePositions);
    } else if (getLayoutManager() instanceof ToroLayoutManager) {
      ToroLayoutManager layoutManager = (ToroLayoutManager) getLayoutManager();
      firstPosition = layoutManager.getFirstVisibleItemPosition();
      lastPosition = layoutManager.getLastVisibleItemPosition();
    }

    if (firstPosition <= lastPosition /* protect the 'for' loop */ &&  //
        (firstPosition != RecyclerView.NO_POSITION || lastPosition != RecyclerView.NO_POSITION)) {
      for (int i = firstPosition; i <= lastPosition; i++) {
        // Detected a view holder for media player
        RecyclerView.ViewHolder viewHolder = findViewHolderForAdapterPosition(i);
        if (viewHolder != null && viewHolder instanceof ToroPlayer) {
          ToroPlayer candidate = (ToroPlayer) viewHolder;
          // check candidate's condition
          if (candidate.wantsToPlay() && getStrategy().allowsToPlay(candidate, this)) {
            // Have a new candidate who can play
            if (!candidates.contains(candidate)) {
              candidates.add(candidate);
            }
          }
        }
      }
    }

    // Ask strategy to elect one
    final ToroPlayer electedPlayer = getStrategy().findBestPlayer(candidates);

    if (electedPlayer == currentPlayer) {
      // No thing changes, no new President. Let it go
      if (currentPlayer != null) {
        if (!currentPlayer.isPrepared()) {
          // We catch the state of prepared and trigger it manually
          currentPlayer.preparePlayer(false);
        } else if (!currentPlayer.isPlaying()) {  // player is prepared and ready to play
          playerManager.restorePlaybackState(currentPlayer.getMediaId());
          playerManager.startPlayback();
        }
      }

      return;
    }

    // Current player is not elected anymore, pause it.
    if (currentPlayer != null && currentPlayer.isPlaying()) {
      playerManager.savePlaybackState(currentPlayer.getMediaId(),
          currentPlayer.getCurrentPosition(), currentPlayer.getDuration());
      playerManager.pausePlayback();
    }

    if (electedPlayer == null) {
      // Old president resigned, but there is no new ones, we are screwed up, get out of here.
      return;
    }

    playerManager.setPlayer(null);  // we allows new player, so first we need to clear current one
    // Well... let's the BlackHouse starts new cycle with the new President!
    if (!electedPlayer.isPrepared()) {
      electedPlayer.preparePlayer(false);
    } else {
      playerManager.setPlayer(electedPlayer);
      playerManager.restorePlaybackState(electedPlayer.getMediaId());
      playerManager.startPlayback();
    }
  }
}
