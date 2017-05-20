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

  @Override public void onChildAttachedToWindow(final View child) {
    super.onChildAttachedToWindow(child);
    ViewHolder holder = getChildViewHolder(child);
    if (holder == null || !(holder instanceof ToroPlayer)) {
      return;
    }

    final ToroPlayer player = (ToroPlayer) holder;

    if (playerManager == null) {
      return;
    }

    if (playerManager.getPlayer() == player) {
      if (!player.isPrepared()) {
        player.preparePlayer(false);
      } else {
        playerManager.restorePlaybackState(player.getMediaId());
        playerManager.startPlayback();
      }
    } else if (playerManager.getPlayer() == null) {
      child.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
        @Override public void onGlobalLayout() {
          child.getViewTreeObserver().removeOnGlobalLayoutListener(this);
          if (player.wantsToPlay() && strategy.allowsToPlay(player, PlayerListView.this)) {
            if (!player.isPrepared()) {
              player.preparePlayer(false);
            } else {
              playerManager.setPlayer(player);
              playerManager.restorePlaybackState(player.getMediaId());
              playerManager.startPlayback();
            }
          }
        }
      });
    }
  }

  // Unused for now
  @Override public void onChildDetachedFromWindow(View child) {
    super.onChildDetachedFromWindow(child);
    ViewHolder holder = getChildViewHolder(child);
    if (holder == null || !(holder instanceof ToroPlayer)) {
      return;
    }

    final ToroPlayer player = (ToroPlayer) holder;

    // Manually save Video state
    if (playerManager != null && player == playerManager.getPlayer()) {
      if (player.isPlaying()) {
        playerManager.savePlaybackState( //
            player.getMediaId(), player.getCurrentPosition(), player.getDuration());
        playerManager.pausePlayback();
      }
      // Detach current Player
      playerManager.setPlayer(null);
    }

    player.stop();
  }

  // handle scrolling events

  private final List<ToroPlayer> candidates = new ArrayList<>();

  // unused
  @Override public void onScrolled(int dx, int dy) {
    super.onScrolled(dx, dy);
  }

  @Override public void onScrollStateChanged(int newState) {
    super.onScrollStateChanged(newState);
    if (newState != RecyclerView.SCROLL_STATE_IDLE) {
      return;
    }

    if (playerManager == null) {
      return;
    }

    // clear current playback candidates
    candidates.clear();
    // Check current playing position
    final ToroPlayer currentPlayer = playerManager.getPlayer();
    if (currentPlayer != null && currentPlayer.getPlayOrder() != RecyclerView.NO_POSITION) {
      if (currentPlayer.wantsToPlay() && this.strategy.allowsToPlay(currentPlayer, this)) {
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
        RecyclerView.ViewHolder holder = findViewHolderForAdapterPosition(i);
        if (holder != null && holder instanceof ToroPlayer) {
          ToroPlayer candidate = (ToroPlayer) holder;
          // check candidate's condition
          if (candidate.wantsToPlay() && this.strategy.allowsToPlay(candidate, this)) {
            // Have a new candidate who can play
            if (!candidates.contains(candidate)) {
              candidates.add(candidate);
            }
          }
        }
      }
    }

    // Ask strategy to elect one
    final ToroPlayer electedPlayer = this.strategy.findBestPlayer(candidates);

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

  private PlayerManager playerManager;
  private ToroStrategy strategy;

  void setPlayerManager(PlayerManager playerManager) {
    if (this.playerManager == playerManager) {
      return;
    }

    ToroPlayer currentPlayer = null;
    PlaybackState currentState = null;
    if (this.playerManager != null) {
      currentPlayer = this.playerManager.getPlayer();
      if (currentPlayer != null) {
        this.playerManager.savePlaybackState(currentPlayer.getMediaId(),
            currentPlayer.getCurrentPosition(), currentPlayer.getDuration());
        this.playerManager.pausePlayback();
        currentState = this.playerManager.getPlaybackState(currentPlayer.getMediaId());
        this.playerManager.setPlayer(null);
      }
    }

    this.playerManager = playerManager;
    if (this.playerManager != null) {
      if (currentPlayer != null && currentState != null) {
        this.playerManager.savePlaybackState(currentState.getMediaId(), currentState.getPosition(),
            currentState.getDuration());
      }
      onScrollStateChanged(RecyclerView.SCROLL_STATE_IDLE);
    }
  }

  PlayerManager getPlayerManager() {
    return playerManager;
  }

  ToroStrategy getStrategy() {
    return strategy;
  }

  void setStrategy(ToroStrategy strategy) {
    if (strategy == null) {
      throw new IllegalArgumentException("Playback strategy must not be null.");
    }

    if (this.strategy == strategy) {
      return;
    }

    this.strategy = strategy;
    if (this.playerManager != null) {
      onScrollStateChanged(RecyclerView.SCROLL_STATE_IDLE);
    }
  }
}
