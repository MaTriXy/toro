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

import android.support.annotation.NonNull;

/**
 * @author eneim.
 * @since 5/19/17.
 */

public final class PlaylistHelper {

  @NonNull private final PlayerManager playerManager;
  @NonNull private ToroStrategy strategy;

  public PlaylistHelper() {
    this(PlayerManager.Factory.getInstance());
  }

  public PlaylistHelper(PlayerManager playerManager) {
    this(playerManager, ToroStrategy.FIRST_PLAYABLE_TOP_DOWN);
  }

  public PlaylistHelper(@NonNull PlayerManager playerManager, @NonNull ToroStrategy strategy) {
    this.playerManager = playerManager;
    this.strategy = strategy;
  }

  @NonNull public ToroStrategy getStrategy() {
    return strategy;
  }

  public void setStrategy(@NonNull ToroStrategy strategy) {
    //noinspection ConstantConditions
    if (strategy == null) {
      throw new IllegalArgumentException("Playback strategy must not be null.");
    }
    this.strategy = strategy;
    if (this.playerListView != null) {
      this.playerListView.setStrategy(strategy);
    }
  }

  // implement new mechanism
  private PlayerListView playerListView;

  public final void registerPlayerListView(PlayerListView view) {
    if (this.playerListView == view) {
      return;
    }

    if (this.playerListView != null) {
      this.playerListView.setPlayerManager(null);
    }

    this.playerListView = view;
    if (this.playerListView != null) {
      if (this.playerListView.getPlayerManager() != null) {
        throw new IllegalStateException("This View is already setup with a PlayerManager.");
      }
      this.playerListView.setStrategy(this.strategy);
      this.playerListView.setPlayerManager(playerManager);
    }
  }
}
