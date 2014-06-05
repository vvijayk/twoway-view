package org.lucasr.twowayview;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class TwoWayGridView extends TwoWayView {
    private static final String LOGTAG = "TwoWayGridView";

    private LayoutState mLayoutState;
    private int mLaneSize;
    private int mLaneCount;
    private boolean mIsVertical;

    public TwoWayGridView(Context context) {
        this(context, null);
    }

    public TwoWayGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TwoWayGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mLaneSize = 0;
        mLaneCount = 3;

        Orientation orientation = getOrientation();
        mLayoutState = new LayoutState(orientation, mLaneCount);
        mIsVertical = (orientation == Orientation.VERTICAL);
    }

    private int getLaneForPosition(int position) {
        return (position % mLaneCount);
    }

    @Override
    public void setOrientation(Orientation orientation) {
        super.setOrientation(orientation);
        mIsVertical = (orientation == Orientation.VERTICAL);
    }

    @Override
    public void offsetLayout(int offset) {
        mLayoutState.offset(offset);
    }

    @Override
    public void resetLayout(int offset) {
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int paddingRight = getPaddingRight();
        final int paddingBottom = getPaddingBottom();

        if (mIsVertical) {
            mLaneSize = (getWidth() - paddingLeft - paddingRight) / mLaneCount;
        } else {
            mLaneSize = (getHeight() - paddingTop - paddingBottom) / mLaneCount;
        }

        for (int i = 0; i < mLaneCount; i++) {
            final int l = paddingLeft + (mIsVertical ? i * mLaneSize : offset);
            final int t = paddingTop + (mIsVertical ? offset : i * mLaneSize);
            final int r = (mIsVertical ? l + mLaneSize : l);
            final int b = (mIsVertical ? t : t + mLaneSize);

            mLayoutState.set(i, l, t, r, b);
        }
    }

    @Override
    public int getOuterStartEdge() {
        return mLayoutState.getOuterStartEdge();
    }

    @Override
    public int getInnerStartEdge() {
        return mLayoutState.getInnerStartEdge();
    }

    @Override
    public int getInnerEndEdge() {
        return mLayoutState.getInnerEndEdge();
    }

    @Override
    public int getOuterEndEdge() {
        return mLayoutState.getOuterEndEdge();
    }

    @Override
    public int getChildWidthMeasureSpec(View child, int position, LayoutParams lp) {
        if (!mIsVertical && lp.width == LayoutParams.WRAP_CONTENT) {
            return MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        } else if (mIsVertical) {
            return MeasureSpec.makeMeasureSpec(mLaneSize, MeasureSpec.EXACTLY);
        } else {
            return MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
        }
    }

    @Override
    public int getChildHeightMeasureSpec(View child, int position, LayoutParams lp) {
        if (mIsVertical && lp.height == LayoutParams.WRAP_CONTENT) {
            return MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        } else if (!mIsVertical) {
            return MeasureSpec.makeMeasureSpec(mLaneSize, MeasureSpec.EXACTLY);
        } else {
            return MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
        }
    }

    @Override
    public void detachChildFromLayout(View child, int position, Flow flow) {
        final int lane = getLaneForPosition(position);

        if (flow == Flow.FORWARD) {
            mLayoutState.offset(lane, mIsVertical ? child.getHeight() : child.getWidth());
        }

        if (mIsVertical) {
            mLayoutState.reduceHeightBy(lane, child.getHeight());
        } else {
            mLayoutState.reduceWidthBy(lane, child.getWidth());
        }
    }

    @Override
    public void attachChildToLayout(View child, int position, Flow flow, boolean needsLayout) {
        final int childWidth = child.getMeasuredWidth();
        final int childHeight = child.getMeasuredHeight();

        final int lane = getLaneForPosition(position);
        final Rect laneState = mLayoutState.get(lane);

        final int l, t, r, b;
        if (mIsVertical) {
            l = laneState.left;
            t = (flow == Flow.FORWARD ? laneState.bottom : laneState.top - childHeight);
            r = laneState.right;
            b = t + childHeight;
        } else {
            l = (flow == Flow.FORWARD ? laneState.right : laneState.left - childWidth);
            t = laneState.top;
            r = l + childWidth;
            b = laneState.bottom;
        }

        if (needsLayout) {
            child.layout(l, t, r, b);
        } else {
            child.offsetLeftAndRight(l - child.getLeft());
            child.offsetTopAndBottom(t - child.getTop());
        }

        if (flow == Flow.BACK) {
            mLayoutState.offset(lane, mIsVertical ? -childHeight : -childWidth);
        }

        if (mIsVertical) {
            mLayoutState.increaseHeightBy(lane, childHeight);
        } else {
            mLayoutState.increaseWidthBy(lane, childWidth);
        }
    }
}
