package com.nuautotest.View;

import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.nuautotest.Activity.TPTestActivity;
import com.nuautotest.application.ModuleTestApplication;

public class TPTestView extends View implements View.OnSystemUiVisibilityChangeListener {
	/* Step 1 */
	private final int mBorder = 1;
	private final int mCombine = 2;
	/* Step 3 */
	private final int tolerance = 80;
	private int width;

	private final Paint mPaint;
	private final Path [] mPath;
	private final Path [] mPathStart;
	private final Path [] mPathEnd;
	private RectF[][] mRect;
	private boolean[][] mPressed;
	private int mNumberPressed, mNumberNeeded;
	private int mRectRow, mRectColumn, mRectWidth, mRectHeight;
	private final Context mContext;
	private int mStep;
	private int mTouchNum;
	private int mLine, mStartX, mStartY, mEndX, mEndY;
	private int mLastSystemUiVis;
	private final int mSdkVersion = Build.VERSION.SDK_INT;

	public TPTestView(Context context, AttributeSet attr) {
		super(context, attr);
		mContext = context;
		mPaint = new Paint();
		mPath = new Path[2];
		mPath[0] = new Path();
		mPath[1] = new Path();
		mPathStart = new Path[2];
		mPathStart[0] = new Path();
		mPathStart[1] = new Path();
		mPathEnd = new Path[2];
		mPathEnd[0] = new Path();
		mPathEnd[1] = new Path();
		mStep = 0;
		if (mSdkVersion >= Build.VERSION_CODES.KITKAT)
			setOnSystemUiVisibilityChangeListener(this);
	}

	void setRectInfo(int scrWidth, int scrHeight) {
		int i, j, w, h, m, mr=0, mc=0;

		m = Math.abs(scrWidth-scrHeight);
		for (i=20; i<=36; i+=2) {
			for (j=20; j<=36; j+=2) {
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

	void init(int w, int h) {
		width = (int) (tolerance * Math.sqrt(w * w + h * h) / h);
		mNumberPressed = 0;
		mStep = 4;

		if (mStep == 4) {
			final float width = (float) w / 9, height = (float) h / 13;
			float s;
			int i;

			mRect = new RectF[7][13];
			mPressed = new boolean[7][13];
			mNumberNeeded = 0;
			mLine = 0;
			mStartX = mStartY = mEndX = mEndY = -1;

			/* Top H */
			mRect[0][0] = new RectF(9, 9, 9, 9);
			for (i = 1; i <= 9; i++) {
				mRect[0][i] = new RectF((i - 1) * width + 1, 1, i * width - 1, height - 1);
				mPressed[0][i] = false;
			}
			mNumberNeeded += 9;

			/* Mid H */
			mRect[1][0] = new RectF(7, 7, 7, 7);
			s = h / 2 - height / 2;
			for (i = 1; i <= 7; i++) {
				mRect[1][i] = new RectF(i * width + 1, s + 1, (i + 1) * width - 1, s + height - 1);
				mPressed[1][i] = false;
			}
			mNumberNeeded += 7;

			/* Bottom H */
			mRect[2][0] = new RectF(9, 9, 9, 9);
			s = h - height;
			for (i = 1; i <= 9; i++) {
				mRect[2][i] = new RectF((i - 1) * width + 1, s + 1, i * width - 1, h - 1);
				mPressed[2][i] = false;
			}
			mNumberNeeded += 9;

			/* Left V */
			mRect[3][0] = new RectF(11, 11, 11, 11);
			for (i = 1; i <= 11; i++) {
				mRect[3][i] = new RectF(1, i * height + 1, width - 1, (i + 1) * height - 1);
				mPressed[3][i] = false;
			}
			mNumberNeeded += 11;

			/* Mid upper V */
			mRect[4][0] = new RectF(5, 5, 5, 5);
			s = w / 2 - width / 2;
			for (i = 1; i <= 5; i++) {
				mRect[4][i] = new RectF(s + 1, i * height + 1, s + width - 1, (i + 1) * height - 1);
				mPressed[4][i] = false;
			}
			mNumberNeeded += 5;

			/* Mid lower V */
			mRect[5][0] = new RectF(5, 5, 5, 5);
			s = w / 2 - width / 2;
			for (i = 1; i <= 5; i++) {
				mRect[5][i] = new RectF(s + 1, h / 2 + height / 2 + (i - 1) * height + 1,
						s + width - 1, h / 2 + height / 2 + i * height - 1);
				mPressed[5][i] = false;
			}
			mNumberNeeded += 5;

			/* Right V */
			mRect[6][0] = new RectF(11, 11, 11, 11);
			s = w - width;
			for (i = 1; i <= 11; i++) {
				mRect[6][i] = new RectF(s + 1, i * height + 1, w - 1, (i + 1) * height - 1);
				mPressed[6][i] = false;
			}
			mNumberNeeded += 11;

			setPath(1, 0);
			setPath(2, 1);
		} else {
			this.setRectInfo(w, h);

			mRect = new RectF[mRectRow][mRectColumn];
			mPressed = new boolean[mRectRow][mRectColumn];
			for (int i = 0; i < mRectRow; i++) {
				for (int j = 0; j < mRectColumn; j++) {
					mRect[i][j] = new RectF();
					mRect[i][j].left = j * mRectWidth + 1;
					if (j == mRectColumn - 1)
						mRect[i][j].right = w;
					else
						mRect[i][j].right = (j + 1) * mRectWidth - 1;
					mRect[i][j].top = i * mRectHeight + 1;
					if (i == mRectRow - 1)
						mRect[i][j].bottom = h;
					else
						mRect[i][j].bottom = (i + 1) * mRectHeight - 1;

					mPressed[i][j] = false;
				}
			}
		}
		this.postInvalidate();
		if (mSdkVersion >= Build.VERSION_CODES.KITKAT)
			setNavVisibility(true);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
//		if (mStep > 1) return;
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
/*			for (int i=0; i<mRectRow; i++)
				for (int j=0; j<mRectColumn; j++) {
					if (mPressed[i][j])
						mPaint.setColor(Color.GREEN);
					else
						mPaint.setColor(Color.RED);
					canvas.drawRect(mRect[i][j], mPaint);
				}
*/
			for (int i=0; i<mRectRow; i++) {
				if (i<mBorder || i>=mRectRow-mBorder) {
					for (int j=0; j<mRectColumn; j++) {
						if (mPressed[i][j])
							mPaint.setColor(Color.GREEN);
						else
							mPaint.setColor(Color.RED);
						canvas.drawRect(mRect[i][j].left, mRect[i][j].top, mRect[i][j].right, mRect[i][j].bottom, mPaint);
					}
				} else {
					for (int j=0; j<mRectColumn; j++) {
						if (mPressed[i][j])
							mPaint.setColor(Color.GREEN);
						else
							mPaint.setColor(Color.RED);
						if (j<mBorder || j>=mRectColumn-mBorder) {
							canvas.drawRect(mRect[i][j], mPaint);
						} else {
							if ((i-mBorder)%mCombine != 0) continue;
							canvas.drawRect(mRect[i][j].left, mRect[i][j].top,
									mRect[i+mCombine-1][j+mCombine-1].right,
									mRect[i+mCombine-1][j+mCombine-1].bottom, mPaint);
							j+=mCombine-1;
						}
					}
				}
			}
		} else if (mStep == 2) {
			mPaint.setColor(Color.WHITE);
			mPaint.setTextSize(30);
			canvas.drawText("多点触摸测试，触摸点："+mTouchNum, 50, 50, mPaint);
		} else if (mStep == 3) {
			mPaint.setColor(Color.GRAY);
			canvas.drawPath(mPath[0], mPaint);
			mPaint.setColor(Color.LTGRAY);
			canvas.drawPath(mPathStart[0], mPaint);
			canvas.drawPath(mPathEnd[0], mPaint);
//			if (mLine == 1)
//				canvas.drawLine(0, 0, this.getWidth(), this.getHeight(), mPaint);
//			else if (mLine == 2)
//				canvas.drawLine(0, this.getHeight(), this.getWidth(), 0, mPaint);
//			else if (mLine == 3)
//				canvas.drawLine(0, 20, this.getWidth(), 20, mPaint);
//			else if (mLine == 4)
//				canvas.drawLine(this.getWidth()-20, 0, this.getWidth()-20, this.getHeight(), mPaint);
//			else if (mLine == 5)
//				canvas.drawLine(0, this.getHeight()-20, this.getWidth(), this.getHeight()-20, mPaint);
//			else if (mLine == 6)
//				canvas.drawLine(20, 0, 20, this.getHeight(), mPaint);
//			else {
//				canvas.drawLine(0, this.getHeight(), this.getWidth(), 0, mPaint);
//				canvas.drawLine(0, 0, this.getWidth(), this.getHeight(), mPaint);
//			}
			if (mStartX != -1 && mStartY != -1) {
				mPaint.setColor(Color.RED);
				mPaint.setStrokeWidth(8.0f);
				canvas.drawLine(mStartX, mStartY, mEndX, mEndY, mPaint);
				mPaint.setStrokeWidth(1.0f);
			}

			mPaint.setColor(Color.WHITE);
			mPaint.setTextSize(30);
			canvas.drawText("对角线测试：请沿屏幕所示区域绘制", 50, 50, mPaint);
		} else if (mStep == 4) {
			for (int i=0; i<7; i++) {
				for (int j=1; j<=mRect[i][0].top; j++) {
					if (mPressed[i][j])
						mPaint.setColor(Color.GREEN);
					else
						mPaint.setColor(Color.RED);
					canvas.drawRect(mRect[i][j], mPaint);
				}
			}

			for (int i=0; i<2; i++) {
				mPaint.setStyle(Paint.Style.STROKE);
				mPaint.setStrokeWidth(3);
				if ((mLine & (1 << (i+2))) != 0)
					mPaint.setColor(Color.rgb(0, 0xaa, 0));
				else
					mPaint.setColor(Color.LTGRAY);
				canvas.drawPath(mPath[i], mPaint);
				canvas.drawPath(mPathStart[i], mPaint);
				canvas.drawPath(mPathEnd[i], mPaint);
			}

			if (mStartX != -1 && mStartY != -1) {
				mPaint.setColor(Color.LTGRAY);
				mPaint.setStrokeWidth(8.0f);
				canvas.drawLine(mStartX, mStartY, mEndX, mEndY, mPaint);
				mPaint.setStrokeWidth(1.0f);
			}
		}
	}

	void setPath(int line, int index) {
		switch (line) {
			case 1:
				mPath[index].reset();
				mPath[index].lineTo(width, 0);
				mPath[index].lineTo(this.getWidth(), this.getHeight() - width);
				mPath[index].lineTo(this.getWidth(), this.getHeight());
				mPath[index].lineTo(this.getWidth() - width, this.getHeight());
				mPath[index].lineTo(0, width);
				mPath[index].close();

				mPathStart[index].reset();
				mPathStart[index].addRect(0, 0, width, width, Path.Direction.CW);

				mPathEnd[index].reset();
				mPathEnd[index].addRect(this.getWidth() - width, this.getHeight() - width, this.getWidth(), this.getHeight(), Path.Direction.CW);
				break;
			case 2:
				mPath[index].reset();
				mPath[index].moveTo(this.getWidth(), 0);
				mPath[index].lineTo(this.getWidth(), width);
				mPath[index].lineTo(width, this.getHeight());
				mPath[index].lineTo(0, this.getHeight());
				mPath[index].lineTo(0, this.getHeight() - width);
				mPath[index].lineTo(this.getWidth() - width, 0);
				mPath[index].close();

				mPathStart[index].reset();
				mPathStart[index].addRect(this.getWidth() - width, 0, this.getWidth(), width, Path.Direction.CW);

				mPathEnd[index].reset();
				mPathEnd[index].addRect(0, this.getHeight() - width, width, this.getHeight(), Path.Direction.CW);
				break;
			case 3:
				mPath[index].reset();
				mPath[index].addRect(0, 0, this.getWidth(), tolerance, Path.Direction.CW);

				mPathStart[index].reset();
				mPathStart[index].addRect(0, 0, tolerance, tolerance, Path.Direction.CW);

				mPathEnd[index].reset();
				mPathEnd[index].addRect(this.getWidth() - tolerance, 0, this.getWidth(), tolerance, Path.Direction.CW);
				break;
			case 4:
				mPath[index].reset();
				mPath[index].addRect(this.getWidth() - tolerance, 0, this.getWidth(), this.getHeight(), Path.Direction.CW);

				mPathStart[index].reset();
				mPathStart[index].addRect(this.getWidth() - tolerance, this.getHeight() - tolerance, this.getWidth(), this.getHeight(), Path.Direction.CW);
				break;
			case 5:
				mPath[index].reset();
				mPath[index].addRect(0, this.getHeight() - tolerance, this.getWidth(), this.getHeight(), Path.Direction.CW);

				mPathEnd[index].reset();
				mPathEnd[index].addRect(0, this.getHeight() - tolerance, tolerance, this.getHeight(), Path.Direction.CW);
				break;
			case 6:
				mPath[index].reset();
				mPath[index].addRect(0, 0, tolerance, this.getHeight(), Path.Direction.CW);

				mPathStart[index].reset();
				mPathStart[index].addRect(0, 0, tolerance, tolerance, Path.Direction.CW);
				break;
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
	double distance(int px, int py, int ax, int ay, int bx, int by) {
		return (Math.abs((px-ax)*(by-ay)-(py-ay)*(bx-ax)))/Math.sqrt((bx-ax)*(bx-ax)+(by-ay)*(by-ay));
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
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
						if (y>=mBorder && y<mRectRow-mBorder && x>=mBorder && x<mRectColumn-mBorder) {
							for (int k=(y-mBorder)/mCombine*mCombine+mBorder;
							     k<=(y-mBorder)/mCombine*mCombine+mBorder+mCombine-1; k++)
								for (int l=(x-mBorder)/mCombine*mCombine+mBorder;
									l<=(x-mBorder)/mCombine*mCombine+mBorder+mCombine-1; l++)
									mPressed[k][l] = true;
							mNumberPressed += mCombine*mCombine;
						} else {
							mPressed[y][x] = true;
							mNumberPressed++;
						}
					}
					postInvalidate();

					if (mNumberPressed == mRectRow * mRectColumn) {
						mStep = 3;
						mStartX = mStartY = mEndX = mEndY = -1;
						mLine = 1;
						setPath(mLine, 0);
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
			switch(mLine) {
				case 1:
					if (mStartX == -1) {
						if ((x<=width && y<=width) || (x>=this.getWidth()-width && y>=this.getHeight()-width)) {
							mStartX = mEndX = x;
							mStartY = mEndY = y;
						}
					} else {
						if (distance(x,y,0,0,this.getWidth(),this.getHeight()) > tolerance) {
							mStartX = mStartY = mEndX = mEndY = -1;
						} else {
							mEndX = x;
							mEndY = y;
							if (Math.abs(mStartX - mEndX) >= this.getWidth() - width * 2 &&
									Math.abs(mStartY - mEndY) >= this.getHeight() - width * 2) {
								mStartX = mStartY = mEndX = mEndY = -1;
								mLine++;
								setPath(mLine, 0);
							}
						}
					}
					break;
				case 2:
					if (mStartX == -1) {
						if ((x<=width && y>=this.getHeight()-width) || (x>=this.getWidth()-width && y<=width)) {
							mStartX = mEndX = x;
							mStartY = mEndY = y;
						}
					} else {
						if (distance(x,y,0,this.getHeight(),this.getWidth(),0) > tolerance) {
							mStartX = mStartY = mEndX = mEndY = -1;
						} else {
							mEndX = x;
							mEndY = y;
							if (Math.abs(mStartX - mEndX) >= this.getWidth() - width * 2 &&
									Math.abs(mStartY - mEndY) >= this.getHeight() - width * 2) {
								mStartX = mStartY = mEndX = mEndY = -1;
								mLine++;
								setPath(mLine, 0);
							}
						}
					}
					break;
				case 3:
					if (mStartX == -1) {
						if (y<=2*tolerance && (x<=tolerance || x>=this.getWidth()-tolerance)) {
							mStartX = mEndX = x;
							mStartY = mEndY = y;
						}
					} else {
						if (y > tolerance) {
							mStartX = mStartY = mEndX = mEndY = -1;
						} else {
							mEndX = x;
							mEndY = y;
							if (Math.abs(mStartX - mEndX) >= this.getWidth() - tolerance * 2 &&
									Math.abs(mStartY - mEndY) <= tolerance * 2) {
								mStartX = mStartY = mEndX = mEndY = -1;
								mLine++;
								setPath(mLine, 0);
							}
						}
					}
					break;
				case 4:
					if (mStartX == -1) {
						if (x>=this.getWidth()-tolerance && (y<=tolerance || y>=this.getHeight()-tolerance)) {
							mStartX = mEndX = x;
							mStartY = mEndY = y;
						}
					} else {
						if (x < this.getWidth() - tolerance) {
							mStartX = mStartY = mEndX = mEndY = -1;
						} else {
							mEndX = x;
							mEndY = y;
							if (Math.abs(mStartX - mEndX) <= tolerance * 2 &&
									Math.abs(mStartY - mEndY) >= this.getHeight() - tolerance * 2) {
								mStartX = mStartY = mEndX = mEndY = -1;
								mLine++;
								setPath(mLine, 0);
							}
						}
					}
					break;
				case 5:
					if (mStartX == -1) {
						if (y>=this.getHeight()-tolerance && (x<=tolerance || x>=this.getWidth()-tolerance)) {
							mStartX = mEndX = x;
							mStartY = mEndY = y;
						}
					} else {
						if (y < this.getHeight() - tolerance) {
							mStartX = mStartY = mEndX = mEndY = -1;
						} else {
							mEndX = x;
							mEndY = y;
							if (Math.abs(mStartX - mEndX) >= this.getWidth() - tolerance * 2 &&
									Math.abs(mStartY - mEndY) <= tolerance * 2) {
								mStartX = mStartY = mEndX = mEndY = -1;
								mLine++;
								setPath(mLine, 0);
							}
						}
					}
					break;
				case 6:
					if (mStartX == -1) {
						if (x<=tolerance && (y<=tolerance || y>=this.getHeight()-tolerance)) {
							mStartX = mEndX = x;
							mStartY = mEndY = y;
						}
					} else {
						if (x > tolerance) {
							mStartX = mStartY = mEndX = mEndY = -1;
						} else {
							mEndX = x;
							mEndY = y;
							if (Math.abs(mStartX - mEndX) <= tolerance * 2 &&
									Math.abs(mStartY - mEndY) >= this.getHeight() - tolerance * 2) {
								mStep = 2;
								mTouchNum = 0;
								Intent intent = new Intent();
								intent.setAction(TPTestActivity.ACTION_TPSHOWBUTTON);
								mContext.sendBroadcast(intent);
							}
						}
					}
					break;
			}
			postInvalidate();
		} else if (mStep == 4) {
			for (int i = 0; i < event.getPointerCount(); i++) {
				for (int j = 0; j <= event.getHistorySize(); j++) {
					int x, y;
					if (j == event.getHistorySize()) {
						x = (int) (event.getX(i));
						y = (int) (event.getY(i));
					} else {
						x = (int) (event.getHistoricalX(i, j));
						y = (int) (event.getHistoricalY(i, j));
					}

					/* Rect fill */
					for (int k = 0; k < 7; k++) {
						for (int l = 1; l <= mRect[k][0].top; l++) {
							if (!mPressed[k][l] && mRect[k][l].contains(x, y)) {
								mPressed[k][l] = true;
								mNumberPressed++;
								break;
							}
						}
					}

					/* Path fill */
					if (event.getAction() == MotionEvent.ACTION_UP) {
						mStartX = mStartY = mEndX = mEndY = -1;
					} else if (mStartX == -1) {
						if ((mLine & 4) == 0 && (x<=width && y<=width ||
								x>=this.getWidth()-width && y>=this.getHeight()-width)) {
							mStartX = mEndX = x;
							mStartY = mEndY = y;
							mLine |= 1;
						}
						if ((mLine & 8) == 0 && (x<=width && y>=this.getHeight()-width ||
								x>=this.getWidth()-width && y<=width)) {
							mStartX = mEndX = x;
							mStartY = mEndY = y;
							mLine |= 2;
						}
					} else {
						if ((mLine & 1) != 0) {
							if (distance(x, y, 0, 0, this.getWidth(), this.getHeight()) > tolerance) {
								mStartX = mStartY = mEndX = mEndY = -1;
								mLine &= ~1;
							} else {
								mEndX = x;
								mEndY = y;
								if (Math.abs(mStartX - mEndX) >= this.getWidth() - width * 2 &&
										Math.abs(mStartY - mEndY) >= this.getHeight() - width * 2) {
									mStartX = mStartY = mEndX = mEndY = -1;
									mLine = (mLine & ~1) | 4;
								}
							}
						} else if ((mLine & 2) != 0) {
							if (distance(x,y,0,this.getHeight(),this.getWidth(),0) > tolerance) {
								mStartX = mStartY = mEndX = mEndY = -1;
								mLine &= ~2;
							} else {
								mEndX = x;
								mEndY = y;
								if (Math.abs(mStartX - mEndX) >= this.getWidth() - width * 2 &&
										Math.abs(mStartY - mEndY) >= this.getHeight() - width * 2) {
									mStartX = mStartY = mEndX = mEndY = -1;
									mLine = (mLine & ~2) | 8;
								}
							}
						}
					}
				}
			}
			postInvalidate();

			if (mNumberPressed == mNumberNeeded && (mLine & 4) != 0 && (mLine & 8) != 0) {
				Intent intent = new Intent();
				intent.setAction(TPTestActivity.ACTION_TPSUCCESS);
				mContext.sendBroadcast(intent);
			}
		}

		return true;
	}

	private final Runnable mNavHider = new Runnable() {
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
