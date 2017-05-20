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

import android.support.annotation.Nullable;
import android.view.ViewParent;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static im.ene.toro.ToroUtil.doAllowsToPlay;

/**
 * Created by eneim on 2/1/16.
 *
 * @since 1.0.0
 */
public interface ToroStrategy {

  /**
   * @return Description of current Strategy
   */
  String getDescription();

  /**
   * Each item of candidates returns true for {@link ToroPlayer#wantsToPlay()}. A
   * Strategy gives the best fit Player to start playing.
   *
   * @param candidates the list of all possible to play {@code ToroPlayer}s.
   * @return the best {@code ToroPlayer} widget to start playback.
   */
  @Nullable ToroPlayer findBestPlayer(List<ToroPlayer> candidates);

  /**
   * Called after {@link ToroPlayer#wantsToPlay()} to verify that current player is
   * allowed to play by current Strategy
   *
   * @param player ToroPlayer object which wants to play, and wait for permission
   * @param parent the RecyclerView that {@code player} is attached to.
   * @return {@code true} if {@code player} suffices all requirements to play, {@code false}
   * otherwise.
   */
  boolean allowsToPlay(ToroPlayer player, ViewParent parent);

  ToroStrategy REST = new ToroStrategy() {
    @Override public String getDescription() {
      return "'Do nothing' Strategy";
    }

    @Override public ToroPlayer findBestPlayer(List<ToroPlayer> candidates) {
      return null;
    }

    @Override public boolean allowsToPlay(ToroPlayer player, ViewParent parent) {
      return false;
    }
  };

  ToroStrategy MOST_VISIBLE_TOP_DOWN = new ToroStrategy() {

    @Override public String getDescription() {
      return "Most visible item, top - down";
    }

    @Nullable @Override public ToroPlayer findBestPlayer(List<ToroPlayer> candidates) {
      if (candidates == null || candidates.size() < 1) {
        return null;
      }

      // 1. Sort candidates by the order of player
      Collections.sort(candidates, new Comparator<ToroPlayer>() {
        @Override public int compare(ToroPlayer lhs, ToroPlayer rhs) {
          return lhs.getPlayOrder() - rhs.getPlayOrder();
        }
      });

      // 2. Sort candidates by the visible offset
      Collections.sort(candidates, new Comparator<ToroPlayer>() {
        @Override public int compare(ToroPlayer lhs, ToroPlayer rhs) {
          return Float.compare(rhs.visibleAreaOffset(), lhs.visibleAreaOffset());
        }
      });

      return candidates.get(0);
    }

    @Override public boolean allowsToPlay(ToroPlayer player, ViewParent parent) {
      return doAllowsToPlay(player, parent);
    }
  };

  ToroStrategy MOST_VISIBLE_TOP_DOWN_KEEP_LAST = new ToroStrategy() {
    @Override public String getDescription() {
      return "Most visible item, top - down. Keep last playing item.";
    }

    @Nullable @Override public ToroPlayer findBestPlayer(List<ToroPlayer> candidates) {
      if (candidates == null || candidates.size() < 1) {
        return null;
      }

      // Sort candidates by the visible offset
      Collections.sort(candidates, new Comparator<ToroPlayer>() {
        @Override public int compare(ToroPlayer lhs, ToroPlayer rhs) {
          return Float.compare(rhs.visibleAreaOffset(), lhs.visibleAreaOffset());
        }
      });

      return candidates.get(0);
    }

    @Override public boolean allowsToPlay(ToroPlayer player, ViewParent parent) {
      return doAllowsToPlay(player, parent);
    }
  };

  ToroStrategy FIRST_PLAYABLE_TOP_DOWN = new ToroStrategy() {
    @Override public String getDescription() {
      return "First playable item, top - down";
    }

    @Nullable @Override public ToroPlayer findBestPlayer(List<ToroPlayer> candidates) {
      if (candidates == null || candidates.size() < 1) {
        return null;
      }

      // 1. Sort candidates by the order of player
      Collections.sort(candidates, new Comparator<ToroPlayer>() {
        @Override public int compare(ToroPlayer lhs, ToroPlayer rhs) {
          return lhs.getPlayOrder() - rhs.getPlayOrder();
        }
      });

      return candidates.get(0);
    }

    @Override public boolean allowsToPlay(ToroPlayer player, ViewParent parent) {
      return doAllowsToPlay(player, parent);
    }
  };

  ToroStrategy FIRST_PLAYABLE_TOP_DOWN_KEEP_LAST = new ToroStrategy() {

    @Override public String getDescription() {
      return "First playable item, top - down. Keep last playing item.";
    }

    @Nullable @Override public ToroPlayer findBestPlayer(List<ToroPlayer> candidates) {
      if (candidates == null || candidates.size() < 1) {
        return null;
      }

      return candidates.get(0);
    }

    @Override public boolean allowsToPlay(ToroPlayer player, ViewParent parent) {
      return doAllowsToPlay(player, parent);
    }
  };
}
