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

package im.ene.toro.sample.feature.extended;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import im.ene.toro.PlayerListView;
import im.ene.toro.PlaylistHelper;
import im.ene.toro.extended.SnapToTopLinearLayoutManager;
import im.ene.toro.sample.BaseToroFragment;
import im.ene.toro.sample.R;

/**
 * Created by eneim on 6/30/16.
 */
public class ExtendedListFragment extends BaseToroFragment {

  protected PlayerListView recyclerView;
  protected ExtendedAdapter adapter;
  private PlaylistHelper playlistHelper;

  public static ExtendedListFragment newInstance() {
    return new ExtendedListFragment();
  }

  @Nullable @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.generic_recycler_view, container, false);
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    recyclerView = (PlayerListView) view.findViewById(R.id.recycler_view);
    SnapToTopLinearLayoutManager layoutManager =
        new SnapToTopLinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.addItemDecoration(
        new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

    adapter = new ExtendedAdapter();
    recyclerView.setAdapter(adapter);

    playlistHelper = new PlaylistHelper(adapter);
  }

  @Override protected void dispatchFragmentActive() {
    super.dispatchFragmentActive();
    playlistHelper.registerPlayerListView(recyclerView);
  }

  @Override protected void dispatchFragmentInactive() {
    super.dispatchFragmentInactive();
    playlistHelper.registerPlayerListView(null);
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    playlistHelper = null;
  }
}
