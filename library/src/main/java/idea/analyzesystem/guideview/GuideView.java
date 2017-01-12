package idea.analyzesystem.guideview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Scroller;
import android.widget.Toast;

@SuppressLint("ClickableViewAccessibility")
public class GuideView extends ViewGroup {

	/**
	 *
	 <attr name="orientation"> <enum name="horizontal" value="1" /> <enum
	 * name="vertical" value="2" /> </attr>
	 **/

	/** 滑动方向 */
	private int mOrientation = 0;

	/** 水平方向 */
	private int mHorientation = 0;

	/** 垂直方向 */
	private int mVertical = 1;

	/** 屏幕宽度 */
	private int mScreenWidth;

	/** 屏幕高度 */
	private int mScreenHeight;

	/** 滑动状态 */
	private boolean isScrolling;

	/** 滑动辅助类 */
	private Scroller mScroller;

	/** 记录当前的x/y的值 */
	private PointF mPointF;

	/** 记录上一次的x、y值 */
	private PointF mLastPointF;

	/** Scroller 对应的开始坐标 */
	private Point mScrollStartPoint;

	/** Scroller 对应的结束坐标 */
	private Point mScrollStopPoint;

	/** 记录滑动的距离 */
	private PointF mDistancePointF;

	/**ScrollXY 的差值*/
	private Point mDistanceScrollPoint;

	/** 加速度检测 */
	private VelocityTracker mVelocityTracker;

	/**切换屏幕时的回调函数*/
	private OnPageChangeListener mOnPageChangeListener;

	/**
	 * 记录当前页
	 */
	private int currentPage = 0;

	public GuideView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
		// 获取自定义属性
		TypedArray mTypeArray = context.obtainStyledAttributes(
				attrs, R.styleable.GuideView);

		mOrientation = mTypeArray.getInteger(
				R.styleable.GuideView_orientation, mOrientation);

		mTypeArray.recycle();
		// 获取屏幕宽高
		initialScreen(context);

		mScroller = new Scroller(context);

		mPointF = new PointF();
		mLastPointF = new PointF();
		mScrollStartPoint = new Point();
		mScrollStopPoint = new Point();
		mDistancePointF = new PointF();
		mDistanceScrollPoint=new Point();
	}

	public GuideView(Context context, AttributeSet attrs) {
		this(context, attrs, 1);
		// TODO Auto-generated constructor stub
	}

	public GuideView(Context context) {
		this(context, null);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		// 获取子布局，重新测量子布局宽高
		int count = getChildCount();
		for (int i = 0; i < count; i++) {
			measureChild(getChildAt(i), widthMeasureSpec, heightMeasureSpec);
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub

		if (changed) {
			// 重新测量layout的位置
			MarginLayoutParams params = (MarginLayoutParams) getLayoutParams();
			int childCount = getChildCount();
			if (mOrientation == mHorientation) {
				params.width = mScreenWidth * getChildCount();
				setLayoutParams(params);

				for (int i = 0; i < childCount; i++) {
					View view = getChildAt(i);
					if (view.getVisibility() != View.GONE) {
						view.layout(i * mScreenWidth, t, i * mScreenWidth
								+ mScreenWidth, b);
					}
				}
			} else if (mOrientation == mVertical) {

				params.height = mScreenHeight * getChildCount();
				setLayoutParams(params);

				for (int i = 0; i < childCount; i++) {
					View view = getChildAt(i);
					// view 没有隐藏掉，就重新定位
					if (view.getVisibility() != View.GONE) {
						view.layout(l, i * mScreenHeight, r, i * mScreenHeight
								+ mScreenHeight);
					}
				}
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		// 先进行事件判断拦截
		if(currentPage==getChildCount()-1){
			Toast.makeText(getContext(), "finish", Toast.LENGTH_SHORT).show();
			return super.onTouchEvent(event);
		}

		if (isScrolling)
			return super.onTouchEvent(event);

		mPointF.x = event.getX();
		mPointF.y = event.getY();

		// 初始化加速度检测器
		initialVelocity(event);

		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			// 当用户触摸时记录下坐标信息
			Log.i("info"," *******mPoint value****"+"x:"+mPointF.x+"y:"+mPointF.y);
			getStartScrollXY();
			mLastPointF.x = mPointF.x;
			mLastPointF.y = mPointF.y;
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			Log.i("info"," *******mLastPoint value****"+"x:"+mLastPointF.x+"y:"+mLastPointF.y);
			Log.i("info"," *******mPoint value****"+"x:"+mPointF.x+"y:"+mPointF.y);
			Log.i("info"," *******************************************");
			Log.i("info"," *******************************************");
			/**
			 * Stops the animation. Contrary to
			 * {@link #forceFinished(boolean)}, aborting the animating cause
			 * the scroller to move to the final x and y position 源码说明：
			 * mScroller.abortAnimation() 如果滑动还没有结束，那么就终止滑动。
			 *
			 * @see #forceFinished(boolean)
			 */
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation();
			}

			mDistancePointF.x = mLastPointF.x - mPointF.x;
			mDistancePointF.y = mLastPointF.y - mPointF.y;

			Log.i("info"," *******mDistancePointF value ******"+"dx: "+mDistancePointF.x+" dy: "+mDistancePointF.y);
			getStopScrollXY();

			// 先判断滑动的方向确定滑动的距离 scrollBy（x,y）

			// 1.y轴---向上滑动--下一个视图
			// 2.y轴---向下滑动--上一个视图
			// 3.x轴---向左滑动--下一个视图
			// 4.x轴---向右滑动--上一个视图

			/**
			 * 320*480 -8 mlasty=-10 currenty=-2
			 * distance=mlasty-currenty=-8《0 scrolly+distance<0?
			 *
			 * 条件都满足时，确定视图向上滑动，加载 下一个视图
			 *
			 * 重新定义distanceY的值以便于ScrollBy(x,y)调用
			 *
			 * 补充说明：
			 * getScrollX()说明:=手机屏幕显示区域左上角x坐标减去MultiViewGroup视图左上角x坐标=320
			 *
			 * getScrollY()说明:=手机屏幕显示区域左上角y坐标减去MultiViewGroup视图左上角y坐标=0(
			 * 因为子视图的高度和手机屏幕高度一样)
			 *
			 *
			 **/

			if (mOrientation == mHorientation) {
				if (mDistancePointF.x > 0
						&& mScrollStopPoint.x + mDistancePointF.x > getWidth()-mScreenWidth) {
					mDistancePointF.x = getWidth() - mScreenWidth -mScrollStopPoint.x;
				} else if (mDistancePointF.x < 0
						&& mScrollStopPoint.x + mDistancePointF.x < 0) {
					mDistancePointF.x = - mScrollStopPoint.x;
				}
				scrollBy((int) mDistancePointF.x, 0);
			} else if (mOrientation == mVertical) {
				if (mDistancePointF.y < 0
						&& mScrollStopPoint.y + mDistancePointF.y < 0) {
					mDistancePointF.y = -mScrollStopPoint.y;
				}
				if (mDistancePointF.y > 0
						&& mScrollStopPoint.y + mDistancePointF.y > getHeight()
						- mScreenHeight) {
					mDistancePointF.y = getHeight() - mScreenHeight
							- mScrollStopPoint.y;
				}
				scrollBy(0, (int) mDistancePointF.y);
			}
			mLastPointF.x = mPointF.x;
			mLastPointF.y = mPointF.y;
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			getStopScrollXY();
			getDistanceScrollXY();
			//比较滑动方向趋势
			//判断是上滑动还是下滑动
			if(checkDirection()){
				//上滑动《加载更多》
				if(isScrollToNext()){
					//能滑动到下一页
					if(mOrientation==mHorientation){
						mScroller.startScroll(getScrollX(), 0,mScreenWidth - mDistanceScrollPoint.x,0);
					}else if (mOrientation==mVertical){
						mScroller.startScroll(0, getScrollY(), 0, mScreenHeight
								- mDistanceScrollPoint.y);
					}
				}else{
					//不能滑动到下一页
					if(mOrientation==mHorientation){
						mScroller.startScroll(getScrollX(), 0,-mDistanceScrollPoint.x,0);
					}else if (mOrientation==mVertical){
						mScroller.startScroll(0, getScrollY(), 0, -mDistanceScrollPoint.y);
					}
				}
			}else{
				//《下滑动，刷新》
				if(isScrollToprivew()){
					//能滑动到上一页
					if(mOrientation==mHorientation){
						mScroller.startScroll( getScrollX(), 0,
								-mScreenWidth - mDistanceScrollPoint.x,0);
					}else if (mOrientation==mVertical){
						mScroller.startScroll(0, getScrollY(), 0,
								-mScreenHeight - mDistanceScrollPoint.y);
					}
				}else{
					//不能滑动到上一页
					if(mOrientation==mHorientation){
						mScroller.startScroll(getScrollX(),0, -mDistanceScrollPoint.x, 0);
					}else if (mOrientation==mVertical){
						mScroller.startScroll(0, getScrollY(), 0, -mDistanceScrollPoint.y);
					}
				}
			}

			isScrolling = true;
			postInvalidate();
			recycleVelocity();
		}

		return true;
	}

	/**
	 * Called by a parent to request that a child update its values for mScrollX
	 * and mScrollY if necessary. This will typically be done if the child is
	 * animating a scroll using a {@link android.widget.Scroller Scroller}
	 * object.
	 *
	 * 为了易于控制滑屏控制，Android框架提供了 computeScroll()方法去控制这个流程。在绘制View时，会在draw()过程调用该
	 * 方法。因此， 再配合使用Scroller实例，我们就可以获得当前应该的偏移坐标，手动使View/ViewGroup偏移至该处。
	 * computeScroll()方法原型如下，该方法位于ViewGroup.java类中
	 */
	@Override
	public void computeScroll() {
		// TODO Auto-generated method stub
		super.computeScroll();

		if (mOrientation== mVertical) {
			if (mScroller.computeScrollOffset()) {
				scrollTo(0, mScroller.getCurrY());
				postInvalidate();
			} else {
				int position = getScrollY() / mScreenHeight;
				if (position != currentPage) {
					if (mOnPageChangeListener != null) {
						currentPage = position;
						mOnPageChangeListener.onPageChange(currentPage);
					}
				}
			}
		} else if (mOrientation== mHorientation) {
			if (mScroller.computeScrollOffset()) {
				scrollTo(mScroller.getCurrX(), 0);
				postInvalidate();
			} else {
				int position = getScrollX() / mScreenWidth;
				if (position != currentPage) {
					if (mOnPageChangeListener != null) {
						currentPage = position;
						mOnPageChangeListener.onPageChange(currentPage);
					}
				}
			}
		}
		isScrolling = false;
	}
	/************************************ Method *********************************************/

	/**
	 * 获取屏幕宽高
	 */
	public void initialScreen(Context context) {
		WindowManager mWindowManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics outMetrics = new DisplayMetrics();
		mWindowManager.getDefaultDisplay().getMetrics(outMetrics);
		mScreenWidth = outMetrics.widthPixels;
		mScreenHeight = outMetrics.heightPixels;
	}

	/**
	 * 初始化加速度检测器
	 *
	 * @param event
	 */
	private void initialVelocity(MotionEvent event) {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(event);
	}
	/**
	 * 初始化scrollX scrollY
	 */
	private void getStartScrollXY(){
		mScrollStartPoint.x = getScrollX();
		mScrollStartPoint.y = getScrollY();
	}
	/**
	 * 停止滑动后的ScrollX ScrollY
	 */
	private void getStopScrollXY(){
		mScrollStopPoint.x = getScrollX();
		mScrollStopPoint.y = getScrollY();
	}
	/**
	 * 比较滑动的ScrollX ScrollY差值
	 */
	private void getDistanceScrollXY(){
		mDistanceScrollPoint.x = mScrollStopPoint.x-mScrollStartPoint.x;
		mDistanceScrollPoint.y = mScrollStopPoint.y-mScrollStartPoint.y;
	}
	/**
	 * 检查滑动方向
	 * @return  true 加载更多  false 刷新
	 */
	public boolean checkDirection(){
		boolean mDirection =false;
		if (mOrientation == mVertical) {
			mDirection = mDistanceScrollPoint.y > 0 ? true : false;
		} else if (mOrientation== mHorientation) {
			mDirection = - mDistanceScrollPoint.x < 0 ? true : false;
		}
		return mDirection;
	}
	/**
	 * 根据滑动距离判断 是否能够滑动到下一屏
	 *  加载跟多
	 * @return
	 */
	private boolean isScrollToNext() {
		boolean isScrollTo = false;
		if (mOrientation == mVertical) {
			isScrollTo = mDistanceScrollPoint.y > mScreenHeight / 2
					|| Math.abs(getVelocity()) > 600;
		} else if (mOrientation == mHorientation) {
			isScrollTo = mDistanceScrollPoint.x > mScreenWidth / 2
					|| Math.abs(getVelocitx()) > 600;
		}
		return isScrollTo;
	}

	/**
	 * 根据滑动距离判断 是否能够滑动到上一屏
	 * 刷新
	 * @return
	 */
	private boolean isScrollToprivew() {
		boolean isScrollTo = false;
		if (mOrientation == mVertical) {
			isScrollTo = -mDistanceScrollPoint.y > mScreenHeight / 2
					|| Math.abs(getVelocity()) > 600;
		} else if (mOrientation == mHorientation) {
			isScrollTo = -mDistanceScrollPoint.x > mScreenWidth / 2
					|| Math.abs(getVelocitx()) > 600;
		}
		return isScrollTo;
	}
	/**
	 * 获取x方向的加速度
	 *
	 * @return
	 */
	private int getVelocitx() {
		mVelocityTracker.computeCurrentVelocity(1000);
		int velocitx = (int) mVelocityTracker.getXVelocity(1000);
		velocitx = (int) mVelocityTracker.getXVelocity(1000);
		return velocitx;
	}
	/**
	 * 获取y方向的加速度
	 *
	 * @return
	 */
	private int getVelocity() {
		mVelocityTracker.computeCurrentVelocity(1000);
		int velocity = (int) mVelocityTracker.getYVelocity(1000);
		velocity = (int) mVelocityTracker.getYVelocity(1000);
		return velocity;
	}
	/**
	 * 释放资源
	 */
	private void recycleVelocity() {
		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}
	/**
	 * 设置回调接口
	 *
	 * @param onPageChangeListener
	 */
	public void setOnPageChangeListener(
			OnPageChangeListener onPageChangeListener) {
		mOnPageChangeListener = onPageChangeListener;
	}

	/**
	 * 回调接口
	 *
	 * @author zhy
	 *
	 */
	public interface OnPageChangeListener {
		void onPageChange(int currentPage);
	}
}
