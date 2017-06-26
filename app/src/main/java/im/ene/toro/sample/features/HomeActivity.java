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

package im.ene.toro.sample.features;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.ViewGroup;
import butterknife.BindView;
import butterknife.ButterKnife;
import im.ene.toro.sample.Playback;
import im.ene.toro.sample.R;
import im.ene.toro.sample.common.BaseActivity;
import java.lang.ref.WeakReference;

import static android.support.v4.view.ViewPager.SCROLL_STATE_IDLE;

/**
 * @author eneim (6/25/17).
 */

public class HomeActivity extends BaseActivity {

  @SuppressWarnings("unused") private static final String TAG = "Toro:HomeActivity";

  @BindView(R.id.pager_tabs) TabLayout tabLayout;
  @BindView(R.id.view_pager) ViewPager viewPager;

  private ViewPager.OnPageChangeListener pageChangeListener;
  DemoPagerAdapter pagerAdapter;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);
    ButterKnife.bind(this);

    pagerAdapter = new DemoPagerAdapter(getSupportFragmentManager());
    viewPager.setAdapter(pagerAdapter);
    pageChangeListener = new PageChangeHelper() {

      int lastPage = -1;

      @Override void dispatchCurrentPageChanged(int currentPage) {
        Log.w(TAG, "dispatchCurrentPageChanged() called with: currentPage = [" + currentPage + "]");
        if (lastPage == currentPage) return;
        Fragment item = pagerAdapter.getPageAt(lastPage);
        if (item != null && item instanceof Playback) {
          ((Playback) item).trigger(false);
        }
        lastPage = currentPage;
        item = pagerAdapter.getPageAt(lastPage);
        if (item != null && item instanceof Playback) {
          ((Playback) item).trigger(true);
        }
      }
    };

    viewPager.addOnPageChangeListener(pageChangeListener);
    tabLayout.setupWithViewPager(viewPager);
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    viewPager.removeOnPageChangeListener(pageChangeListener);
    pageChangeListener = null;
    tabLayout.setupWithViewPager(null);
  }

  static class DemoPagerAdapter extends FragmentStatePagerAdapter {

    SparseArray<WeakReference<Fragment>> cache = new SparseArray<>();

    DemoPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override public Fragment getItem(int position) {
      return Deck.create(Deck.Slides.values()[position].fragmentClass);
    }

    @Override public Object instantiateItem(ViewGroup container, int position) {
      Fragment item = (Fragment) super.instantiateItem(container, position);
      cache.put(position, new WeakReference<>(item));
      return item;
    }

    @Override public void destroyItem(ViewGroup container, int position, Object object) {
      cache.remove(position);
      super.destroyItem(container, position, object);
    }

    @Override public int getCount() {
      return Deck.Slides.values().length;
    }

    @Override public CharSequence getPageTitle(int position) {
      return Deck.Slides.values()[position].title;
    }

    @Nullable Fragment getPageAt(int position) {
      WeakReference<Fragment> cached = cache.get(position);
      if (cached != null) return cached.get();
      return null;
    }

    @Override public float getPageWidth(int position) {
      return 0.75f;
    }
  }

  static abstract class PageChangeHelper extends ViewPager.SimpleOnPageChangeListener {

    private static final int MSG_LAYOUT_STABLE = -100;  // Negative, to prevent conflict
    private static final int DELAY = 50;

    private boolean firstScroll = true;
    private int currentPage = -1;

    private Handler handler = new Handler(msg -> {
      dispatchCurrentPageChanged(msg.arg1);
      return true;
    });

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
      if (firstScroll) {
        currentPage = position;
        handler.removeMessages(MSG_LAYOUT_STABLE);
        firstScroll = false;
      }
    }

    @Override public void onPageSelected(int position) {
      if (currentPage != position) {
        currentPage = position;
        handler.removeMessages(MSG_LAYOUT_STABLE);
      }
    }

    @Override public void onPageScrollStateChanged(int state) {
      super.onPageScrollStateChanged(state);
      if (state == SCROLL_STATE_IDLE) {
        handler.sendMessage(handler.obtainMessage(MSG_LAYOUT_STABLE, currentPage, 0));
      }
    }

    abstract void dispatchCurrentPageChanged(int currentPage);
  }
}
