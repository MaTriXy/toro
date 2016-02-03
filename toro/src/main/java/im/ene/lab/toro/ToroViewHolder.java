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

package im.ene.lab.toro;

import android.media.MediaPlayer;
import android.support.annotation.CallSuper;
import android.view.View;

/**
 * Created by eneim on 1/31/16.
 */
abstract class ToroViewHolder extends ToroAdapter.ViewHolder
    implements ToroPlayer, View.OnLongClickListener {

  private final ToroItemViewHelper mHelper;

  public ToroViewHolder(View itemView) {
    super(itemView);
    mHelper = Toro.RECYCLER_VIEW_HELPER;
  }

  @CallSuper @Override public void onAttachedToParent() {
    super.onAttachedToParent();
    mHelper.onAttachedToParent(this, itemView, itemView.getParent());
  }

  @CallSuper @Override public void onDetachedFromParent() {
    super.onDetachedFromParent();
    mHelper.onDetachedFromParent(this, itemView, itemView.getParent());
  }

  /**
   * Implement from {@link MediaPlayer.OnPreparedListener#onPrepared(MediaPlayer)}
   */
  @CallSuper @Override public void onPrepared(MediaPlayer mp) {
    Toro.onPrepared(this, itemView, itemView.getParent(), mp);
  }

  /**
   * Implement from {@link MediaPlayer.OnCompletionListener#onCompletion(MediaPlayer)}
   */
  @CallSuper @Override public void onCompletion(MediaPlayer mp) {
    Toro.onCompletion(this, mp);
  }

  /**
   * Implement from {@link MediaPlayer.OnErrorListener#onError(MediaPlayer, int, int)}. This method
   * is closed and called only by {@link Toro#onError(ToroPlayer, MediaPlayer, int, int)}, Client
   * should use {@link ToroPlayer#onPlaybackError(MediaPlayer, int, int)}
   */
  @CallSuper @Override public final boolean onError(MediaPlayer mp, int what, int extra) {
    return Toro.onError(this, mp, what, extra);
  }

  /**
   * Implement from {@link MediaPlayer.OnInfoListener#onInfo(MediaPlayer, int, int)}
   */
  @CallSuper @Override public boolean onInfo(MediaPlayer mp, int what, int extra) {
    return Toro.onInfo(this, mp, what, extra);
  }

  /**
   * Implement from {@link MediaPlayer.OnSeekCompleteListener#onSeekComplete(MediaPlayer)}
   */
  @CallSuper @Override public void onSeekComplete(MediaPlayer mp) {
    Toro.onSeekComplete(this, mp);
  }

  @Override public int getPlayOrder() {
    return getAdapterPosition();
  }

  @Override public boolean onLongClick(View v) {
    return mHelper.onItemLongClick(this, itemView, itemView.getParent());
  }
}
