package com.nuautotest.View;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.nuautotest.application.ModuleTestApplication;

public class TPTestView extends View {

	private Paint mPaint;
	private Rect[][] mRect;
	private boolean[][] mPressed;
	private int mNumberPressed;
	private int mRectRow, mRectColumn, mRectWidth, mRectHeight;
	private Context mContext;
	private int mStep;
	private int mTouchNum;
	private int mLine, mStartX, mStartY, mEndX, mEndY;

	public TPTestView(Context context, AttributeSet attr) {
		super(context, attr);
		mContext = context;
		mPaint = new Paint();
		mStep = 0;
	}

	protected void setRectInfo(int scrWidth, int scrHeight) {
		int factor=1, i, j;
		int [][] eachfactor = new int[11][2];

		for (i=1; i<11; i++) {
			eachfactor[i][0] = 0;
			eachfactor[i][1] = 0;
		}

		i=2;
		while (scrWidth>1) {
			while (i<10) {
				while (scrWidth%i==0) {
					eachfactor[i][0]++;
					scrWidth/=i;
				}
				i++;
			}
			scrWidth--;
			i=2;
		}
		scrWidth = 1;

		i=2;
		while (scrHeight>1) {
			while (i<10) {
				while (scrHeight%i==0) {
					eachfactor[i][1]++;
					scrHeight/=i;
				}
				i++;
			}
			scrHeight--;
			i=2;
		}
		scrHeight=1;

		for (i=2; i<11; i++) {
			for (j=0; j<Math.min(eachfactor[i][0],eachfactor[i][1]); j++)
				factor*=i;
			eachfactor[i][0]-=j;
			eachfactor[i][1]-=j;
			for (j=0; j<eachfactor[i][0]; j++)
				scrWidth*=i;
			for (j=0; j<eachfactor[i][1]; j++)
				scrHeight*=i;
		}
//		while ( (scrWidth>10) || (scrHeight>10) ) {
//			i = (scrWidth<scrHeight)?scrWidth:scrHeight;
//			while (i>1) {
//				while ( (scrWidth%i==0) && (scrHeight%i==0) ) {
//					factor*=i;
//					scrWidth/=i;
//					scrHeight/=i;
//				}
//				i--;
//			}
//			Log.i("XieHang", "======"+scrWidth+" "+scrHeight+" "+factor+"======");
//			if ( (scrWidth>10) && (scrWidth>scrHeight) )
//				scrWidth--;
//			else if ( (scrHeight>10) && (scrHeight>scrWidth) )
//				scrHeight--;
//		}
		mRectRow = factor;
		mRectColumn = factor;
		mRectWidth = scrWidth;
		mRectHeight = scrHeight;

		if (scrWidth>scrHeight) {
			i=2;
			while (mRectRow%i!=0) i++;
			while (Math.abs(mRectWidth-mRectHeight*i)<mRectWidth-mRectHeight) {
				mRectHeight*=i;
				mRectRow/=i;
				while (mRectRow%i!=0) i++;
			}
		} else {
			i=2;
			while (mRectColumn%i!=0) i++;
			while (Math.abs(mRectHeight-mRectWidth*i)<mRectHeight-mRectWidth) {
				mRectWidth*=i;
				mRectColumn/=i;
				while (mRectColumn%i!=0) i++;
			}
		}

		i=2;
		while ( (mRectRow>8) || (mRectColumn>8) ) {
			while ( (i<mRectRow) && (i<mRectColumn) &&
					( (mRectRow%i!=0) || (mRectColumn%i!=0) ) ) i++;
			if (mRectRow/i<=2 || mRectColumn/i<=2) break;
			mRectRow/=i;
			mRectColumn/=i;
			mRectWidth*=i;
			mRectHeight*=i;
		}

		if (mRectWidth != (float)this.getWidth()/(float)mRectColumn)
			mRectWidth = this.getWidth()/mRectColumn;
		if (mRectHeight != (float)this.getHeight()/(float)mRectRow)
			mRectHeight = this.getHeight()/mRectRow;
	}

	protected void init(int w, int h) {
		this.setRectInfo(w, h);
//		mRectRow = 4;
//		mRectColumn = 8;
//		mRectWidth = 120;
//		mRectHeight = 100;

		mRect = new Rect[mRectRow][mRectColumn];
		mPressed = new boolean[mRectRow][mRectColumn];
		for (int i=0; i<mRectRow; i++)
			for (int j=0; j<mRectColumn; j++) {
				mRect[i][j] = new Rect();
				mRect[i][j].left = j*mRectWidth+1;
				mRect[i][j].right = (j+1)*mRectWidth-1;
				mRect[i][j].top = i*mRectHeight+1;
				mRect[i][j].bottom = (i+1)*mRectHeight-1;

				mPressed[i][j] = false;
			}
		mNumberPressed = 0;
//		mStartX = mStartY = mEndX = mEndY = -1;
//		mLine = 1;
		mStep = 1;
		this.postInvalidate();
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
				canvas.drawLine(mStartX, mStartY, mEndX, mEndY, mPaint);
			}
		}
	}

	@Override
	public boolean onTouchEvent (MotionEvent event) {
		System.out.println(event.getAction());
		if (mStep == 1) {
			int x = (int) (event.getX()/mRectWidth);
			int y = (int) (event.getY()/mRectHeight);
			if ( (y>=0) && (y<mRectRow) && (x>=0) && (x<mRectColumn) && (!mPressed[y][x]) ) {
				mPressed[y][x] = true;
				mNumberPressed++;
			}
			postInvalidate();

			if (mNumberPressed == mRectRow*mRectColumn) {
				mStep = 3;
				mStartX = mStartY = mEndX = mEndY = -1;
				mLine = 1;
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
			int t = 100;
			if (mStartX == -1) {
				if (mLine == 1) {
					if ( (x<=t && y<=t) || (x>=this.getWidth()-t && y>=this.getHeight()-t) ) {
						mStartX = x;
						mStartY = y;
					}
				} else if (mLine == 2) {
					if ( (x<=t && y>=this.getHeight()-t) || (x>=this.getWidth()-t && y<=t) ) {
						mStartX = x;
						mStartY = y;
					}
				} else if (mLine == 3) {
					if (y<=t && (x<=t || x>=this.getWidth()-t)) {
						mStartX = x;
						mStartY = y;
					}
				} else if (mLine == 4) {
					if (x>=this.getWidth()-t && (y<=t || y>=this.getHeight()-t)) {
						mStartX = x;
						mStartY = y;
					}
				} else if (mLine == 5) {
					if (y>=this.getHeight()-t && (x<=t || x>=this.getWidth()-t)) {
						mStartX = x;
						mStartY = y;
					}
				} else if (mLine == 6) {
					if (x<=t && (y<=t || y>=this.getHeight()-t)) {
						mStartX = x;
						mStartY = y;
					}
				}
			}
			mEndX = x;
			mEndY = y;
			postInvalidate();
			if (mStartX != -1 && mStartY != -1) {
				if (mLine==1 &&
						Math.abs(mStartX-mEndX)>=this.getWidth()-t*2 &&
						Math.abs(mStartY-mEndY)>=this.getHeight()-t*2) {
					mStartX = mStartY = mEndX = mEndY = -1;
					mLine++;
				} else if (mLine==2 &&
						Math.abs(mStartX-mEndX)>=this.getWidth()-t*2 &&
						Math.abs(mStartY-mEndY)>=this.getHeight()-t*2) {
					mStartX = mStartY = mEndX = mEndY = -1;
					mLine++;
				} else if ((mLine==3 || mLine==5) &&
						Math.abs(mStartX-mEndX)>=this.getWidth()-t*2 &&
						Math.abs(mStartY-mEndY)<=t*2) {
					mStartX = mStartY = mEndX = mEndY = -1;
					mLine++;
				} else if (mLine==4 &&
						Math.abs(mStartX-mEndX)<=t*2 &&
						Math.abs(mStartY-mEndY)>=this.getHeight()-t*2) {
					mStartX = mStartY = mEndX = mEndY = -1;
					mLine++;
				} else if (mLine==6 &&
						Math.abs(mStartX-mEndX)<=t*2 &&
						Math.abs(mStartY-mEndY)>=this.getHeight()-t*2) {
					mStep = 2;
					mTouchNum = 0;
					Intent intent = new Intent();
					intent.setAction(com.nuautotest.Activity.TPTestActivity.ACTION_TPSHOWBUTTON);
					mContext.sendBroadcast(intent);
				}
			}
		}

		return true;
	}
}
