package com.nuautotest.View;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.nuautotest.application.ModuleTestApplication;

public class TPTestView extends View implements View.OnSystemUiVisibilityChangeListener {

	private Paint mPaint;
	private Rect[][] mRect;
	private boolean[][] mPressed;
	private int mNumberPressed;
	private int mRectRow, mRectColumn, mRectWidth, mRectHeight;
	private Context mContext;
	private int mStep;
	private int mTouchNum;
	private int mLine, mStartX, mStartY, mEndX, mEndY;
	int mLastSystemUiVis;
	private int mSdkVersion = Build.VERSION.SDK_INT;

	public TPTestView(Context context, AttributeSet attr) {
		super(context, attr);
		mContext = context;
		mPaint = new Paint();
		mStep = 0;
		if (mSdkVersion >= Build.VERSION_CODES.KITKAT)
			setOnSystemUiVisibilityChangeListener(this);
	}

	protected void setRectInfo(int scrWidth, int scrHeight) {
		int i, j, w, h, m, mr=0, mc=0;

		m = Math.abs(scrWidth-scrHeight);
		for (i=20; i<=35; i++) {
			for (j=20; j<=35; j++) {
				w = scrWidth/i;
				h = scrHeight/j;
				if (Math.abs(w-h)+scrWidth%i+scrHeight%j<m) {
					m = Math.abs(w-h)+scrWidth%i+scrHeight%j;
					mc = i;
					mr = j;
				}
			}
		}
		mRectRow = mr;
		mRectColumn = mc;
		mRectWidth = scrWidth/mc;
		mRectHeight = scrHeight/mr;
	}

	protected void init(int w, int h) {
		this.setRectInfo(w, h);

		mRect = new Rect[mRectRow][mRectColumn];
		mPressed = new boolean[mRectRow][mRectColumn];
		for (int i=0; i<mRectRow; i++)
			for (int j=0; j<mRectColumn; j++) {
				mRect[i][j] = new Rect();
				mRect[i][j].left = j*mRectWidth+1;
				if (j == mRectColumn-1)
					mRect[i][j].right = w;
				else
					mRect[i][j].right = (j+1)*mRectWidth-1;
				mRect[i][j].top = i*mRectHeight+1;
				if (i == mRectRow-1)
					mRect[i][j].bottom = h;
				else
					mRect[i][j].bottom = (i+1)*mRectHeight-1;

				mPressed[i][j] = false;
			}
		mNumberPressed = 0;
		mStep = 1;
		this.postInvalidate();
		if (mSdkVersion >= Build.VERSION_CODES.KITKAT)
			setNavVisibility(true);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (mStep > 1) return;
		if (mStep != 0) Log.d(ModuleTestApplication.TAG, "Old w="+mRectWidth+" h="+mRectHeight);
		this.init(w, h);
		Log.d(ModuleTestApplication.TAG, "New w="+mRectWidth+" h="+mRectHeight);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		canvas.drawColor(Color.BLACK);
		mPaint.setAntiAlias(true);
		mPaint.setStyle(Paint.Style.FILL);

		if (mStep == 0) init(this.getWidth(), this.getHeight());
		else if (mStep == 1) {
			for (int i=0; i<mRectRow; i++)
				for (int j=0; j<mRectColumn; j++) {
					if (mPressed[i][j])
						mPaint.setColor(Color.GREEN);
					else
						mPaint.setColor(Color.RED);
					canvas.drawRect(mRect[i][j], mPaint);
				}
		} else if (mStep == 2) {
			mPaint.setColor(Color.WHITE);
			mPaint.setTextSize(30);
			canvas.drawText("多点触摸测试，触摸点："+mTouchNum, 50, 50, mPaint);
		} else if (mStep == 3) {
			mPaint.setColor(Color.WHITE);
			mPaint.setTextSize(30);
			canvas.drawText("对角线测试：请沿屏幕所示线条绘制", 50, 50, mPaint);
			if (mLine == 1)
				canvas.drawLine(0, 0, this.getWidth(), this.getHeight(), mPaint);
			else if (mLine == 2)
				canvas.drawLine(0, this.getHeight(), this.getWidth(), 0, mPaint);
			else if (mLine == 3)
				canvas.drawLine(0, 20, this.getWidth(), 20, mPaint);
			else if (mLine == 4)
				canvas.drawLine(this.getWidth()-20, 0, this.getWidth()-20, this.getHeight(), mPaint);
			else if (mLine == 5)
				canvas.drawLine(0, this.getHeight()-20, this.getWidth(), this.getHeight()-20, mPaint);
			else if (mLine == 6)
				canvas.drawLine(20, 0, 20, this.getHeight(), mPaint);
			else {
				canvas.drawLine(0, this.getHeight(), this.getWidth(), 0, mPaint);
				canvas.drawLine(0, 0, this.getWidth(), this.getHeight(), mPaint);
			}
			if (mStartX != -1 && mStartY != -1) {
				mPaint.setColor(Color.RED);
				mPaint.setStrokeWidth(8.0f);
				canvas.drawLine(mStartX, mStartY, mEndX, mEndY, mPaint);
				mPaint.setStrokeWidth(1.0f);
			}
		}
	}

	/**
	 * Calculate the distance from point P(px, py) to line AB(ax,ay)->(bx,by)
	 * @param px X coordinate of P
	 * @param py Y coordinate of P
	 * @param ax X coordinate of A
	 * @param ay Y coordinate of A
	 * @param bx X coordinate of B
	 * @param by Y coordinate of B
	 * @return distance P->AB
	 */
	public double distance(int px, int py, int ax, int ay, int bx, int by) {
		return (Math.abs((px-ax)*(by-ay)-(py-ay)*(bx-ax)))/Math.sqrt((bx-ax)*(bx-ax)+(by-ay)*(by-ay));
	}

	@Override
	public boolean onTouchEvent (MotionEvent event) {
		if (mStep == 1) {
			for (int i=0; i<event.getPointerCount(); i++) {
				for (int j=0; j<=event.getHistorySize(); j++) {
					int x, y;
					if (j == event.getHistorySize()) {
						x = (int) (event.getX(i)/* * event.getXPrecision()*/ / mRectWidth);
						y = (int) (event.getY(i)/* * event.getYPrecision()*/ / mRectHeight);
					} else {
						x = (int) (event.getHistoricalX(i,j)/* * event.getXPrecision()*/ / mRectWidth);
						y = (int) (event.getHistoricalY(i,j)/* * event.getYPrecision()*/ / mRectHeight);
					}
					if ((y >= 0) && (y < mRectRow) && (x >= 0) && (x < mRectColumn) && (!mPressed[y][x])) {
						mPressed[y][x] = true;
						mNumberPressed++;
					}
					postInvalidate();

					if (mNumberPressed == mRectRow * mRectColumn) {
						mStep = 3;
						mStartX = mStartY = mEndX = mEndY = -1;
						mLine = 1;
					}
				}
			}
		} else if (mStep == 2) {
			mTouchNum = event.getPointerCount();
			if (event.getAction() == MotionEvent.ACTION_UP) mTouchNum--;
			postInvalidate();
//			if (mTouchNum >= 5) {
//				mStep = 3;
//				mStartX = mStartY = mEndX = mEndY = -1;
//				mLine = 1;
//			}
		} else if (mStep == 3) {
			if (event.getAction() == MotionEvent.ACTION_UP) {
				mStartX = mStartY = mEndX = mEndY = -1;
				return true;
			}
			int x = (int)event.getX();
			int y = (int)event.getY();
			int t = 30;
			switch(mLine) {
				case 1:
					if (mStartX == -1) {
						if ((x<=t && y<=t) || (x>=this.getWidth()-t && y>=this.getHeight()-t)) {
							mStartX = mEndX = x;
							mStartY = mEndY = y;
						}
					} else {
						if (distance(x,y,0,0,this.getWidth(),this.getHeight()) > t) {
							mStartX = mStartY = mEndX = mEndY = -1;
						} else {
							mEndX = x;
							mEndY = y;
							if (Math.abs(mStartX - mEndX) >= this.getWidth() - t * 2 &&
									Math.abs(mStartY - mEndY) >= this.getHeight() - t * 2) {
								mStartX = mStartY = mEndX = mEndY = -1;
								mLine++;
							}
						}
					}
					break;
				case 2:
					if (mStartX == -1) {
						if ((x<=t && y>=this.getHeight()-t) || (x>=this.getWidth()-t && y<=t)) {
							mStartX = mEndX = x;
							mStartY = mEndY = y;
						}
					} else {
						if (distance(x,y,0,this.getHeight(),this.getWidth(),0) > t) {
							mStartX = mStartY = mEndX = mEndY = -1;
						} else {
							mEndX = x;
							mEndY = y;
							if (Math.abs(mStartX - mEndX) >= this.getWidth() - t * 2 &&
									Math.abs(mStartY - mEndY) >= this.getHeight() - t * 2) {
								mStartX = mStartY = mEndX = mEndY = -1;
								mLine++;
							}
						}
					}
					break;
				case 3:
					if (mStartX == -1) {
						if (y<=t && (x<=t || x>=this.getWidth()-t)) {
							mStartX = mEndX = x;
							mStartY = mEndY = y;
						}
					} else {
						if (distance(x,y,t,t,this.getWidth()-t,t) > t) {
							mStartX = mStartY = mEndX = mEndY = -1;
						} else {
							mEndX = x;
							mEndY = y;
							if (Math.abs(mStartX - mEndX) >= this.getWidth() - t * 2 &&
									Math.abs(mStartY - mEndY) <= t * 2) {
								mStartX = mStartY = mEndX = mEndY = -1;
								mLine++;
							}
						}
					}
					break;
				case 4:
					if (mStartX == -1) {
						if (x>=this.getWidth()-t && (y<=t || y>=this.getHeight()-t)) {
							mStartX = mEndX = x;
							mStartY = mEndY = y;
						}
					} else {
						if (distance(x,y,this.getWidth()-t,t,this.getWidth()-t,this.getHeight()-t) > t) {
							mStartX = mStartY = mEndX = mEndY = -1;
						} else {
							mEndX = x;
							mEndY = y;
							if (Math.abs(mStartX - mEndX) <= t * 2 &&
									Math.abs(mStartY - mEndY) >= this.getHeight() - t * 2) {
								mStartX = mStartY = mEndX = mEndY = -1;
								mLine++;
							}
						}
					}
					break;
				case 5:
					if (mStartX == -1) {
						if (y>=this.getHeight()-t && (x<=t || x>=this.getWidth()-t)) {
							mStartX = mEndX = x;
							mStartY = mEndY = y;
						}
					} else {
						if (distance(x,y,t,this.getHeight()-t,this.getWidth()-t,this.getHeight()-t) > t) {
							mStartX = mStartY = mEndX = mEndY = -1;
						} else {
							mEndX = x;
							mEndY = y;
							if (Math.abs(mStartX - mEndX) >= this.getWidth() - t * 2 &&
									Math.abs(mStartY - mEndY) <= t * 2) {
								mStartX = mStartY = mEndX = mEndY = -1;
								mLine++;
							}
						}
					}
					break;
				case 6:
					if (mStartX == -1) {
						if (x<=t && (y<=t || y>=this.getHeight()-t)) {
							mStartX = mEndX = x;
							mStartY = mEndY = y;
						}
					} else {
						if (distance(x,y,t,t,t,this.getHeight()-t) > t) {
							mStartX = mStartY = mEndX = mEndY = -1;
						} else {
							mEndX = x;
							mEndY = y;
							if (Math.abs(mStartX - mEndX) <= t * 2 &&
									Math.abs(mStartY - mEndY) >= this.getHeight() - t * 2) {
								mStep = 2;
								mTouchNum = 0;
								Intent intent = new Intent();
								intent.setAction(com.nuautotest.Activity.TPTestActivity.ACTION_TPSHOWBUTTON);
								mContext.sendBroadcast(intent);
							}
						}
					}
					break;
			}
			postInvalidate();
		}

		return true;
	}

	Runnable mNavHider = new Runnable() {
		@Override public void run() {
			setNavVisibility(false);
		}
	};

	void setNavVisibility(boolean visible) {
		int newVis = SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| SYSTEM_UI_FLAG_LAYOUT_STABLE;
		if (!visible) {
			newVis |= SYSTEM_UI_FLAG_LOW_PROFILE | SYSTEM_UI_FLAG_FULLSCREEN
					| SYSTEM_UI_FLAG_HIDE_NAVIGATION | SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		}

		// If we are now visible, schedule a timer for us to go invisible.
		if (visible) {
			Handler h = getHandler();
			if (h != null) {
				h.removeCallbacks(mNavHider);
				h.postDelayed(mNavHider, 0);
			}
		}

		// Set the new desired visibility.
		setSystemUiVisibility(newVis);
	}

	@Override
	public void onSystemUiVisibilityChange(int visibility) {
		// Detect when we go out of nav-hidden mode, to clear our state
		// back to having the full UI chrome up.  Only do this when
		// the state is changing and nav is no longer hidden.
		int diff = mLastSystemUiVis ^ visibility;
		mLastSystemUiVis = visibility;
		if ((diff&SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
				&& (visibility&SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
			setNavVisibility(true);
		}
	}
}
