/*
 * Copyright (c) 2017 Nam Nguyen, nam@ene.im
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

package im.ene.toro.sample.features.stateful;

import android.os.Bundle;
import android.support.annotation.IntegerRes;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.support.v7.widget.helper.ItemTouchHelper.SimpleCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import im.ene.toro.sample.Playback;
import im.ene.toro.sample.R;
import im.ene.toro.sample.common.BaseFragment;
import im.ene.toro.sample.common.ContentAdapter;
import im.ene.toro.sample.common.EndlessScroller;
import im.ene.toro.sample.data.DataSource;
import im.ene.toro.widget.PlayerSelector;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static android.support.v7.widget.helper.ItemTouchHelper.DOWN;
import static android.support.v7.widget.helper.ItemTouchHelper.LEFT;
import static android.support.v7.widget.helper.ItemTouchHelper.RIGHT;
import static android.support.v7.widget.helper.ItemTouchHelper.UP;

/**
 * @author eneim | 6/8/17.
 */

public class StatefulPlaylistFragment extends BaseFragment implements Playback {

  @SuppressWarnings("unused") public static StatefulPlaylistFragment newInstance() {
    Bundle args = new Bundle();
    StatefulPlaylistFragment fragment = new StatefulPlaylistFragment();
    fragment.setArguments(args);
    return fragment;
  }

  @BindView(R.id.swipe_refresh_layout) SwipeRefreshLayout refreshLayout;
  @BindView(R.id.recycler_view) MyContainer container;
  ContentAdapter adapter;
  ItemTouchHelper touchHelper;
  RecyclerView.OnScrollListener infiniteScrollListener;

  @SuppressWarnings("SpellCheckingInspection")  //
  final CompositeDisposable disposibles = new CompositeDisposable();

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle bundle) {
    return inflater.inflate(R.layout.layout_container_custom, container, false);
  }

  int getInteger(@IntegerRes int intRes) {
    return getResources().getInteger(intRes);
  }

  @Override public void onViewCreated(View view, @Nullable Bundle bundle) {
    super.onViewCreated(view, bundle);
    adapter = new StatefulContentAdapter();
    if (bundle != null) adapter.addMany(true, DataSource.getInstance().getEntities());  // restore.

    container.setAdapter(adapter);
    refreshLayout.setOnRefreshListener(() -> {
      refreshLayout.setRefreshing(true);
      dispatchLoadData(false);
    });

    infiniteScrollListener =
        new EndlessScroller(container.getLayoutManager(), DataSource.getInstance()) {
          @Override public void onLoadMore() {
            if (refreshLayout != null) {
              refreshLayout.setRefreshing(true);
            }
            dispatchLoadData(true);
          }
        };
    container.addOnScrollListener(infiniteScrollListener);
    playerSelector = container.getPlayerSelector();

    touchHelper = new ItemTouchHelper(new SimpleCallback(UP | DOWN | LEFT | RIGHT, 0) {
      @Override public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
          RecyclerView.ViewHolder target) {
        return adapter.onItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
      }

      @Override public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        // do nothing.
      }
    });
    touchHelper.attachToRecyclerView(container);
  }

  @Override public void onViewStateRestored(@Nullable Bundle bundle) {
    super.onViewStateRestored(bundle);
    // Only fetch for completely new data if this Fragment is created from scratch.
    if (bundle == null) dispatchLoadData(false);
  }

  void dispatchLoadData(boolean loadMore) {
    DataSource.getInstance()
        .getFromCloud(loadMore, 50)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnComplete(() -> {
          if (refreshLayout != null && refreshLayout.isRefreshing()) {
            refreshLayout.setRefreshing(false);
          }
        })
        .doOnSubscribe(disposibles::add)
        .subscribe(entities -> adapter.addMany(!loadMore, entities));
  }

  @Override public void onDestroyView() {
    disposibles.clear();  // Clear but not dispose, by intent
    touchHelper.attachToRecyclerView(null);
    touchHelper = null;
    adapter = null;
    super.onDestroyView();
  }

  // Playback interface
  PlayerSelector playerSelector;  // cache

  @Override public void trigger(boolean active) {
    if (active) {
      container.setPlayerSelector(playerSelector);
    } else {
      container.setPlayerSelector(PlayerSelector.NONE);
    }
  }
}
