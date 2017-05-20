/*
 * Copyright 2016 eneim@Eneim Labs, nam@ene.im
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

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

/**
 * Created by eneim on 2/1/16.
 *
 * A helper class to support Video's callbacks from {@link RecyclerView.Adapter}
 */
public abstract class PlayerViewHelper {

  private static final String TAG = "ToroLib:Helper";

  protected final ToroPlayer player;
  protected final View itemView;

  public PlayerViewHelper(@NonNull ToroPlayer player, @NonNull View itemView) {
    this.player = player;
    this.itemView = itemView;
  }

  /* BEGIN: Callback for View */

  /**
   * Callback from {@link RecyclerView.Adapter#onViewAttachedToWindow(RecyclerView.ViewHolder)}
   *
   * Once a view is attached to its parent's window, and there is a {@link PlayerManager} for its
   * parent, this Helper observes the view's layout state and prepares the player or starts the
   * playback if possible.
   */
  @CallSuper public void onAttachedToWindow() {
    ViewParent viewParent = itemView.getParent();
    if (!(viewParent instanceof PlayerListView)) {
      return;
    }
    final PlayerListView playerListView = (PlayerListView) viewParent;
    final PlayerManager manager = playerListView.getPlayerManager();
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
      itemView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
        @Override public void onGlobalLayout() {
          itemView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
          if (player.wantsToPlay() && //
              playerListView.getStrategy().allowsToPlay(player, itemView.getParent())) {
            //noinspection Duplicates
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

  /**
   * Callback from {@link RecyclerView.Adapter#onViewDetachedFromWindow(RecyclerView.ViewHolder)}
   */
  @CallSuper public void onDetachedFromWindow() {
    PlayerManager manager = itemView.getParent() instanceof PlayerListView
        ? ((PlayerListView) itemView.getParent()).getPlayerManager() : null;
    // Manually save Video state
    if (manager != null && manager.getPlayer() == player) {
      if (player.isPlaying()) {
        manager.savePlaybackState( //
            player.getMediaId(), player.getCurrentPosition(), player.getDuration());
        manager.pausePlayback();
      }
      // Detach current Player
      manager.setPlayer(null);
    }

    player.stop();
  }

  @CallSuper public void onBound() {
    //
  }

  @CallSuper public void onRecycled() {
    // hmm, somehow this method doesn't work really well
  }

  /* BEGIN: Callback for MediaPlayer */

  /**
   * @param itemView main View of current ViewHolder
   * @param parent parent which holds current ViewHolder
   */
  @CallSuper protected void onPrepared(@NonNull View itemView, @Nullable ViewParent parent) {
    if (!(parent instanceof PlayerListView)) {
      return;
    }

    PlayerListView playerListView = (PlayerListView) parent;

    if (!player.wantsToPlay() || !playerListView.getStrategy().allowsToPlay(player, parent)) {
      return;
    }

    PlayerManager manager = playerListView.getPlayerManager();
    if (manager == null) {
      return;
    }

    // 1. Check if current playerManager is managing this player
    if (player == manager.getPlayer()) {
      // player.isPlaying() is always false here
      manager.restorePlaybackState(player.getMediaId());
      manager.startPlayback();
    } else {
      // There is no current player, but this guy is prepared, so let's him go ...
      if (manager.getPlayer() == null) {
        // ... if it's possible
        manager.setPlayer(player);
        // player.isPrepared() is always true here
        manager.restorePlaybackState(player.getMediaId());
        manager.startPlayback();
      }
    }
  }

  @SuppressWarnings("WeakerAccess") @Nullable
  protected final PlayerManager getPlayerManager(ViewParent parent) {
    return parent != null && parent instanceof PlayerListView
        ? ((PlayerListView) parent).getPlayerManager() : null;
  }

  protected final ToroStrategy getStrategy(ViewParent parent) {
    return parent != null && parent instanceof PlayerListView
        ? ((PlayerListView) parent).getStrategy() : null;
  }

  /**
   * Complete the playback
   */
  @CallSuper protected void onCompletion() {
    // 1. Internal jobs: find the playerManager for corresponding player.
    PlayerManager manager = getPlayerManager(itemView.getParent());
    // Update video position as 0
    if (manager != null) {
      manager.savePlaybackState(player.getMediaId(), 0L, player.getDuration());
      manager.setPlayer(null);
    }
  }

  protected final boolean onPlaybackError(Exception error) {
    boolean childHandled = this.player.onPlaybackError(error);
    if (childHandled) {
      PlayerManager manager = getPlayerManager(itemView.getParent());
      if (manager != null) {
        manager.savePlaybackState(player.getMediaId(), 0L, player.getDuration());
        manager.pausePlayback();
        manager.setPlayer(null);
      }

      return true;
    } else {
      return false;
    }
  }
}
