package com.martiandeveloper.listwithswipe.tools;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.ListView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecyclerViewTouchListener implements RecyclerView.OnItemTouchListener, OnActivityTouchListener {

    Activity activity;

    List<Integer> unSweapleRows;
    List<Integer> independentViews;
    List<Integer> unClickableRows;
    List<Integer> optionViews;
    Set<Integer> ignoredViewTypes;

    private final int touchSlop;
    private final int minFlingVel;
    private final int maxFlingVel;
    private final long ANIMATION_CLOSE = 150;

    private final RecyclerView recyclerView;

    private int bgWidth = 1;

    private float touchedX;
    private float touchedY;
    private boolean isFgSwiping;
    private int mSwipingSlop;
    private VelocityTracker mVelocityTracker;
    private int touchedPosition;
    private View touchedView;
    private boolean mPaused;
    private boolean bgVisible, fgPartialViewClicked;
    private int bgVisiblePosition;
    private View bgVisibleView;
    private boolean isRViewScrolling;
    private int heightOutsideRView, screenHeight;
    private View fgView;
    private View bgView;

    private int fgViewID;
    private int bgViewID;
    private final ArrayList<Integer> fadeViews;
    private OnRowClickListener mRowClickListener;
    private OnSwipeOptionsClickListener mBgClickListener;
    // user choices
    private boolean clickable = false;
    private boolean swipeable = false;

    public RecyclerViewTouchListener(Activity a, RecyclerView recyclerView) {
        this.activity = a;
        ViewConfiguration vc = ViewConfiguration.get(recyclerView.getContext());
        touchSlop = vc.getScaledTouchSlop();
        minFlingVel = vc.getScaledMinimumFlingVelocity() * 16;
        maxFlingVel = vc.getScaledMaximumFlingVelocity();
        this.recyclerView = recyclerView;
        bgVisible = false;
        bgVisiblePosition = -1;
        bgVisibleView = null;
        fgPartialViewClicked = false;
        unSweapleRows = new ArrayList<>();
        unClickableRows = new ArrayList<>();
        ignoredViewTypes = new HashSet<>();
        independentViews = new ArrayList<>();
        optionViews = new ArrayList<>();
        fadeViews = new ArrayList<>();
        isRViewScrolling = false;

        this.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                setEnabled(newState != RecyclerView.SCROLL_STATE_DRAGGING);
                isRViewScrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

            }
        });
    }

    public void setEnabled(boolean enabled) {
        mPaused = !enabled;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent motionEvent) {
        return handleTouchEvent(motionEvent);
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent motionEvent) {
        handleTouchEvent(motionEvent);
    }

    public RecyclerViewTouchListener setClickable(OnRowClickListener listener) {
        this.clickable = true;
        this.mRowClickListener = listener;
        return this;
    }

    public RecyclerViewTouchListener setSwipeable(int foregroundID, int backgroundID, OnSwipeOptionsClickListener listener) {
        this.swipeable = true;
        if (fgViewID != 0 && foregroundID != fgViewID)
            throw new IllegalArgumentException("foregroundID does not match previously set ID");
        fgViewID = foregroundID;
        bgViewID = backgroundID;
        this.mBgClickListener = listener;

        if (activity instanceof RecyclerTouchListenerHelper)
            ((RecyclerTouchListenerHelper) activity).setOnActivityTouchListener(this);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        screenHeight = displaymetrics.heightPixels;

        return this;
    }

    public RecyclerViewTouchListener setSwipeOptionViews(Integer... viewIds) {
        this.optionViews = new ArrayList<>(Arrays.asList(viewIds));
        return this;
    }

    private boolean isIndependentViewClicked(MotionEvent motionEvent) {
        for (int i = 0; i < independentViews.size(); i++) {
            if (touchedView != null) {
                Rect rect = new Rect();
                int x = (int) motionEvent.getRawX();
                int y = (int) motionEvent.getRawY();
                touchedView.findViewById(independentViews.get(i)).getGlobalVisibleRect(rect);
                if (rect.contains(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private int getOptionViewID(MotionEvent motionEvent) {
        for (int i = 0; i < optionViews.size(); i++) {
            if (touchedView != null) {
                Rect rect = new Rect();
                int x = (int) motionEvent.getRawX();
                int y = (int) motionEvent.getRawY();
                touchedView.findViewById(optionViews.get(i)).getGlobalVisibleRect(rect);
                if (rect.contains(x, y)) {
                    return optionViews.get(i);
                }
            }
        }
        return -1;
    }

    private int getIndependentViewID(MotionEvent motionEvent) {
        for (int i = 0; i < independentViews.size(); i++) {
            if (touchedView != null) {
                Rect rect = new Rect();
                int x = (int) motionEvent.getRawX();
                int y = (int) motionEvent.getRawY();
                touchedView.findViewById(independentViews.get(i)).getGlobalVisibleRect(rect);
                if (rect.contains(x, y)) {
                    return independentViews.get(i);
                }
            }
        }
        return -1;
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    @Deprecated
    public void closeVisibleBG() {
        if (bgVisibleView == null) {
            return;
        }
        bgVisibleView.animate()
                .translationX(0)
                .setDuration(ANIMATION_CLOSE)
                .setListener(null);

        animateFadeViews(bgVisibleView, 1f, ANIMATION_CLOSE);
        bgVisible = false;
        bgVisibleView = null;
        bgVisiblePosition = -1;
    }

    public void closeVisibleBG(final OnSwipeListener mSwipeCloseListener) {
        if (bgVisibleView == null) {
            return;
        }
        final ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(bgVisibleView,
                View.TRANSLATION_X, 0f);
        translateAnimator.setDuration(ANIMATION_CLOSE);
        translateAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mSwipeCloseListener != null)
                    mSwipeCloseListener.onSwipeOptionsClosed();
                translateAnimator.removeAllListeners();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        translateAnimator.start();

        animateFadeViews(bgVisibleView, 1f, ANIMATION_CLOSE);
        bgVisible = false;
        bgVisibleView = null;
        bgVisiblePosition = -1;
    }

    private void animateFadeViews(View downView, float alpha, long duration) {
        if (fadeViews != null) {
            for (final int viewID : fadeViews) {
                downView.findViewById(viewID).animate()
                        .alpha(alpha)
                        .setDuration(duration);
            }
        }
    }

    private void animateFG(View downView, Animation animateType, long duration) {
        if (animateType == Animation.OPEN) {
            ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(
                    fgView, View.TRANSLATION_X, -bgWidth);
            translateAnimator.setDuration(duration);
            translateAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
            translateAnimator.start();
            animateFadeViews(downView, 0f, duration);
        } else if (animateType == Animation.CLOSE) {
            ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(
                    fgView, View.TRANSLATION_X, 0f);
            translateAnimator.setDuration(duration);
            translateAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
            translateAnimator.start();
            animateFadeViews(downView, 1f, duration);
        }
    }

    private void animateFG(View downView, final Animation animateType, long duration,
                           final OnSwipeListener mSwipeCloseListener) {
        final ObjectAnimator translateAnimator;
        if (animateType == Animation.OPEN) {
            translateAnimator = ObjectAnimator.ofFloat(fgView, View.TRANSLATION_X, -bgWidth);
            translateAnimator.setDuration(duration);
            translateAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
            translateAnimator.start();
            animateFadeViews(downView, 0f, duration);
        } else {
            translateAnimator = ObjectAnimator.ofFloat(fgView, View.TRANSLATION_X, 0f);
            translateAnimator.setDuration(duration);
            translateAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
            translateAnimator.start();
            animateFadeViews(downView, 1f, duration);
        }

        translateAnimator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {

                if (mSwipeCloseListener != null) {
                    if (animateType == Animation.OPEN)
                        mSwipeCloseListener.onSwipeOptionsOpened();
                    else if (animateType == Animation.CLOSE)
                        mSwipeCloseListener.onSwipeOptionsClosed();
                }
                translateAnimator.removeAllListeners();

            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

        });

    }

    private boolean handleTouchEvent(MotionEvent motionEvent) {

        if (swipeable && bgWidth < 2) {
            if (activity.findViewById(bgViewID) != null)
                bgWidth = activity.findViewById(bgViewID).getWidth();

            heightOutsideRView = screenHeight - recyclerView.getHeight();
        }

        long ANIMATION_STANDARD = 300;
        switch (motionEvent.getActionMasked()) {

            case MotionEvent.ACTION_DOWN: {
                if (mPaused) {
                    break;
                }

                Rect rect = new Rect();
                int childCount = recyclerView.getChildCount();
                int[] listViewCoords = new int[2];
                recyclerView.getLocationOnScreen(listViewCoords);
                int x = (int) motionEvent.getRawX() - listViewCoords[0];
                int y = (int) motionEvent.getRawY() - listViewCoords[1];
                View child;

                for (int i = 0; i < childCount; i++) {

                    child = recyclerView.getChildAt(i);
                    child.getHitRect(rect);
                    if (rect.contains(x, y)) {
                        touchedView = child;
                        break;
                    }

                }

                if (touchedView != null) {

                    touchedX = motionEvent.getRawX();
                    touchedY = motionEvent.getRawY();
                    touchedPosition = recyclerView.getChildAdapterPosition(touchedView);

                    if (shouldIgnoreAction(touchedPosition)) {
                        touchedPosition = ListView.INVALID_POSITION;
                        return false;
                    }

                    if (swipeable) {

                        mVelocityTracker = VelocityTracker.obtain();
                        mVelocityTracker.addMovement(motionEvent);
                        fgView = touchedView.findViewById(fgViewID);
                        bgView = touchedView.findViewById(bgViewID);
                        bgView.setMinimumHeight(fgView.getHeight());

                        if (bgVisible && fgView != null) {
                            x = (int) motionEvent.getRawX();
                            y = (int) motionEvent.getRawY();
                            fgView.getGlobalVisibleRect(rect);
                            fgPartialViewClicked = rect.contains(x, y);
                        } else {
                            fgPartialViewClicked = false;
                        }

                    }

                }

                recyclerView.getHitRect(rect);

                if (swipeable && bgVisible && touchedPosition != bgVisiblePosition) {
                    closeVisibleBG(null);
                }

                break;

            }

            case MotionEvent.ACTION_CANCEL: {

                if (mVelocityTracker == null) {
                    break;
                }

                if (swipeable) {
                    if (touchedView != null && isFgSwiping) {
                        animateFG(touchedView, Animation.CLOSE, ANIMATION_STANDARD);
                    }
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                    isFgSwiping = false;
                    bgView = null;
                }

                touchedX = 0;
                touchedY = 0;
                touchedView = null;
                touchedPosition = ListView.INVALID_POSITION;
                break;

            }

            case MotionEvent.ACTION_UP: {

                if (mVelocityTracker == null && swipeable) {
                    break;
                }

                if (touchedPosition < 0)
                    break;

                boolean swipedLeft = false;
                boolean swipedRight = false;

                boolean swipedLeftProper = false;
                boolean swipedRightProper = false;

                float mFinalDelta = motionEvent.getRawX() - touchedX;

                if (isFgSwiping) {
                    swipedLeft = mFinalDelta < 0;
                    swipedRight = mFinalDelta > 0;
                }

                if (Math.abs(mFinalDelta) > bgWidth / 2 && isFgSwiping) {
                    swipedLeftProper = mFinalDelta < 0;
                    swipedRightProper = mFinalDelta > 0;
                } else if (swipeable) {

                    mVelocityTracker.addMovement(motionEvent);
                    mVelocityTracker.computeCurrentVelocity(1000);
                    float velocityX = mVelocityTracker.getXVelocity();
                    float absVelocityX = Math.abs(velocityX);
                    float absVelocityY = Math.abs(mVelocityTracker.getYVelocity());
                    if (minFlingVel <= absVelocityX && absVelocityX <= maxFlingVel
                            && absVelocityY < absVelocityX && isFgSwiping) {
                        swipedLeftProper = (velocityX < 0) == (mFinalDelta < 0);
                        swipedRightProper = (velocityX > 0) == (mFinalDelta > 0);
                    }

                }

                if (swipeable && !swipedRight && swipedLeftProper && touchedPosition != RecyclerView.NO_POSITION
                        && !unSweapleRows.contains(touchedPosition) && !bgVisible) {

                    final int downPosition = touchedPosition;
                    animateFG(touchedView, Animation.OPEN, ANIMATION_STANDARD);
                    bgVisible = true;
                    bgVisibleView = fgView;
                    bgVisiblePosition = downPosition;
                } else if (swipeable && !swipedLeft && swipedRightProper && touchedPosition != RecyclerView.NO_POSITION
                        && !unSweapleRows.contains(touchedPosition) && bgVisible) {
                    animateFG(touchedView, Animation.CLOSE, ANIMATION_STANDARD);
                    bgVisible = false;
                    bgVisibleView = null;
                    bgVisiblePosition = -1;
                } else if (swipeable && swipedLeft && !bgVisible) {

                    final View tempBgView = bgView;
                    animateFG(touchedView, Animation.CLOSE, ANIMATION_STANDARD, new OnSwipeListener() {

                        @Override
                        public void onSwipeOptionsClosed() {
                            if (tempBgView != null)
                                tempBgView.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onSwipeOptionsOpened() {
                        }

                    });

                    bgVisible = false;
                    bgVisibleView = null;
                    bgVisiblePosition = -1;

                } else if (swipeable && swipedRight && bgVisible) {
                    animateFG(touchedView, Animation.OPEN, ANIMATION_STANDARD);
                    bgVisible = true;
                    bgVisibleView = fgView;
                    bgVisiblePosition = touchedPosition;
                } else if (swipeable && swipedRight && !bgVisible) {
                    animateFG(touchedView, Animation.CLOSE, ANIMATION_STANDARD);
                    bgVisible = false;
                    bgVisibleView = null;
                    bgVisiblePosition = -1;
                } else if (swipeable && swipedLeft && bgVisible) {
                    animateFG(touchedView, Animation.OPEN, ANIMATION_STANDARD);
                    bgVisible = true;
                    bgVisibleView = fgView;
                    bgVisiblePosition = touchedPosition;
                } else if (!swipedRight && !swipedLeft) {

                    if (swipeable && fgPartialViewClicked) {
                        animateFG(touchedView, Animation.CLOSE, ANIMATION_STANDARD);
                        bgVisible = false;
                        bgVisibleView = null;
                        bgVisiblePosition = -1;
                    } else if (clickable && !bgVisible && touchedPosition >= 0 && !unClickableRows.contains(touchedPosition)
                            && isIndependentViewClicked(motionEvent) && !isRViewScrolling) {
                        mRowClickListener.onRowClicked(touchedPosition);
                    } else if (clickable && !bgVisible && touchedPosition >= 0 && !unClickableRows.contains(touchedPosition)
                            && !isIndependentViewClicked(motionEvent) && !isRViewScrolling) {
                        final int independentViewID = getIndependentViewID(motionEvent);
                        if (independentViewID >= 0)
                            mRowClickListener.onIndependentViewClicked(independentViewID, touchedPosition);
                    } else if (swipeable && bgVisible && !fgPartialViewClicked) {

                        final int optionID = getOptionViewID(motionEvent);

                        if (optionID >= 0 && touchedPosition >= 0) {

                            final int downPosition = touchedPosition;

                            closeVisibleBG(new OnSwipeListener() {

                                @Override
                                public void onSwipeOptionsClosed() {
                                    mBgClickListener.onSwipeOptionClicked(optionID, downPosition);
                                }

                                @Override
                                public void onSwipeOptionsOpened() {
                                }

                            });

                        }

                    }

                }

            }

            if (swipeable) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }

            touchedX = 0;
            touchedY = 0;
            touchedView = null;
            touchedPosition = ListView.INVALID_POSITION;
            isFgSwiping = false;
            bgView = null;
            break;

            case MotionEvent.ACTION_MOVE: {

                if (mVelocityTracker == null || mPaused || !swipeable) {
                    break;
                }

                mVelocityTracker.addMovement(motionEvent);
                float deltaX = motionEvent.getRawX() - touchedX;
                float deltaY = motionEvent.getRawY() - touchedY;

                if (!isFgSwiping && Math.abs(deltaX) > touchSlop && Math.abs(deltaY) < Math.abs(deltaX) / 2) {
                    isFgSwiping = true;
                    mSwipingSlop = (deltaX > 0 ? touchSlop : -touchSlop);
                }

                if (swipeable && isFgSwiping && !unSweapleRows.contains(touchedPosition)) {

                    if (bgView == null) {
                        bgView = touchedView.findViewById(bgViewID);
                        bgView.setVisibility(View.VISIBLE);
                    }

                    if (deltaX < touchSlop && !bgVisible) {

                        float translateAmount = deltaX - mSwipingSlop;

                        fgView.setTranslationX(Math.abs(translateAmount) > bgWidth ? -bgWidth : translateAmount);
                        if (fgView.getTranslationX() > 0) fgView.setTranslationX(0);

                        if (fadeViews != null) {

                            for (int viewID : fadeViews) {
                                touchedView.findViewById(viewID).setAlpha(1 - (Math.abs(translateAmount) / bgWidth));
                            }

                        }

                    } else if (deltaX > 0 && bgVisible) {

                        if (bgVisible) {

                            float translateAmount = (deltaX - mSwipingSlop) - bgWidth;

                            fgView.setTranslationX(translateAmount > 0 ? 0 : translateAmount);

                            if (fadeViews != null) {

                                for (int viewID : fadeViews) {
                                    touchedView.findViewById(viewID).setAlpha(1 - (Math.abs(translateAmount) / bgWidth));
                                }

                            }

                        } else {

                            float translateAmount = (deltaX - mSwipingSlop) - bgWidth;

                            fgView.setTranslationX(translateAmount > 0 ? 0 : translateAmount);

                            if (fadeViews != null) {

                                for (int viewID : fadeViews) {
                                    touchedView.findViewById(viewID).setAlpha(1 - (Math.abs(translateAmount) / bgWidth));
                                }

                            }

                        }

                    }

                    return true;

                } else if (swipeable && isFgSwiping && unSweapleRows.contains(touchedPosition)) {

                    if (deltaX < touchSlop && !bgVisible) {
                        float translateAmount = deltaX - mSwipingSlop;
                        if (bgView == null)
                            bgView = touchedView.findViewById(bgViewID);

                        if (bgView != null)
                            bgView.setVisibility(View.GONE);

                        fgView.setTranslationX(translateAmount / 5);
                        if (fgView.getTranslationX() > 0) fgView.setTranslationX(0);
                    }

                    return true;

                }

                break;

            }

        }

        return false;

    }

    @Override
    public void getTouchCoordinates(MotionEvent ev) {
        int y = (int) ev.getRawY();
        if (swipeable && bgVisible && ev.getActionMasked() == MotionEvent.ACTION_DOWN
                && y < heightOutsideRView) closeVisibleBG(null);
    }

    private boolean shouldIgnoreAction(int touchedPosition) {
        return recyclerView == null || ignoredViewTypes.contains(recyclerView.getAdapter().getItemViewType(touchedPosition));
    }

    private enum Animation {
        OPEN, CLOSE
    }

    public interface OnRowClickListener {
        void onRowClicked(int position);

        void onIndependentViewClicked(int independentViewID, int position);
    }

    public interface OnSwipeOptionsClickListener {
        void onSwipeOptionClicked(int viewID, int position);
    }

    public interface RecyclerTouchListenerHelper {
        void setOnActivityTouchListener(OnActivityTouchListener listener);
    }

    public interface OnSwipeListener {
        void onSwipeOptionsClosed();

        void onSwipeOptionsOpened();
    }

}
