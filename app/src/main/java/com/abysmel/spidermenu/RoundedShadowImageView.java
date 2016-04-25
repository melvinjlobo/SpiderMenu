package com.abysmel.spidermenu;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by Melvin Lobo on 4/14/2016.
 *
 * Will display an ImageView cropped on an oval and with a shadow.
 *
 * A note about BitmapShader:
 * BitmapShader is used to create a source of pixels from a bitmap instead of a color. You can then
 * draw any shapes you want with that bitmap shader which will actually use the source bitmap as the
 * paint to draw the shape. Voila', you have your shaped bitmap
 */
public class RoundedShadowImageView extends ImageView implements View.OnClickListener {
	/////////////////////////////////////// CLASS MEMBERS //////////////////////////////////////////
	/**
	 * Static definitions
	 */
	private static final int DEFAULT_BLUR_RADIUS = 3;
	private static final int DEFAULT_SHADOW_ELEVATION = 7;
	private static final float MAX_ELEVATION_LEVELS = 10;
	private static final int DEFAULT_BACKGROUND_COLOR = Color.parseColor( "#FFFFFF" );
	private static final int DEFAULT_SHADOW_COLOR = Color.parseColor( "#555555" );

	/**
	 * The Bitmap Shader to draw the circular bitmap
	 */
	private BitmapShader mBitmapShader = null;

	/**
	 * The pain to draw the bitmap with the Bitmap Shader
	 */
	private Paint mBitmapPaint = new Paint( Paint.ANTI_ALIAS_FLAG );

	/**
	 * The Paint to draw the shadow
	 */
	private Paint mShadowPaint = new Paint( Paint.ANTI_ALIAS_FLAG );

	/**
	 * Background Fill Paint
	 */
	private Paint mBackgroundPaint = new Paint( Paint.ANTI_ALIAS_FLAG );

	/**
	 * The shadow radius
	 */
	private int mnShadowRadius = 0;

	/**
	 * A reference of the drawable for this image view. Useful for us so that we do not initializeView the bitmap
	 * more than is required
	 */
	private Drawable mDrawable = null;

	/**
	 * The bitmap to be rendered
	 */
	private Bitmap mBitmap = null;

	/**
	 * The horizontal offset of the shadow from the bitmap
	 */
	private int mnHorizontalOffset = 0;

	/**
	 * The vertical offset of the shadow from the bitmap
	 */
	private int mnVerticalOffset = 0;

	/**
	 * The bitmap diameter
	 */
	private int mnBitmapDiameter = 0;

	/**
	 * The elevation of the shadow. The lower the number, the higher the object
	 */
	private float mnObjectElevation = 0;

	/**
	 * Check if horizontal offset is required
	 */
	private boolean mbIsHorizontalOffsetRequired = false;

	/**
	 * Check if vertical offset is required
	 */
	private boolean mbIsVerticalOffsetRequired = true;

	/**
	 * The background fill color
	 */
	private int mBackgroundColor = DEFAULT_BACKGROUND_COLOR;

	/**
	 * The shadow color
	 */
	private int mShadowColor = 0;

	/**
	 * The View Click Listener
	 */
	private ViewClickListener mClickListener = null;

	/**
	 * Indicates whether the bitmap should be resized or not. True by default
	 */
	private boolean mbShouldResizeBitmap = true;

	/////////////////////////////////////// CLASS METHODS //////////////////////////////////////////

	/**
	 * Constructors
	 */
	public RoundedShadowImageView( Context context ) {
		this( context, null );
	}

	public RoundedShadowImageView( Context context, AttributeSet attrs ) {
		this ( context, attrs, 0 );
	}

	public RoundedShadowImageView( Context context, AttributeSet attrs, int defStyleAttr ) {
		super( context, attrs, defStyleAttr );
		initializeView(context, attrs);
	}

	public RoundedShadowImageView(Context context, int nDrawableResourceID) {
		this( context, null );
		setImageDrawable( ContextCompat.getDrawable( context, nDrawableResourceID ) );
		initializeView(context, null);
	}

	@TargetApi( Build.VERSION_CODES.LOLLIPOP )
	public RoundedShadowImageView( Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes ) {
		super( context, attrs, defStyleAttr, defStyleRes );
		initializeView(context, attrs);
	}

	/**
	 * Initialize the Bitmap Shader and the paints
	 *
	 * @author Melvin Lobo
	 */
	private void initializeView(Context context, AttributeSet attrs) {
		if(attrs != null) {
			if(attrs != null) {
				TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RoundedShadowImageView, 0, 0);
				mnShadowRadius = a.getDimensionPixelOffset( R.styleable.RoundedShadowImageView_shadowRadius, DEFAULT_BLUR_RADIUS );
				mnObjectElevation = a.getDimensionPixelOffset( R.styleable.RoundedShadowImageView_shadowElevation, DEFAULT_SHADOW_ELEVATION );
				mBackgroundColor = a.getColor( R.styleable.RoundedShadowImageView_backgroundFillColor, DEFAULT_BACKGROUND_COLOR );
				mbIsHorizontalOffsetRequired = a.getBoolean( R.styleable.RoundedShadowImageView_horizontalShadowOffsetRequired, false );
				mbIsVerticalOffsetRequired = a.getBoolean( R.styleable.RoundedShadowImageView_veticalShadowOffsetRequired, false );
				mShadowColor = a.getColor( R.styleable.RoundedShadowImageView_shadowColor, DEFAULT_SHADOW_COLOR );
				mbShouldResizeBitmap = a.getBoolean( R.styleable.RoundedShadowImageView_shouldResizeBitmap, true );

				a.recycle();
			}
		}
		mShadowPaint.setDither( true );
		mBitmapPaint.setDither( true );
		mBackgroundPaint.setDither( true );

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			setLayerType( LAYER_TYPE_SOFTWARE, mShadowPaint );
			setLayerType( LAYER_TYPE_SOFTWARE, mBitmapPaint );
			setLayerType( LAYER_TYPE_SOFTWARE, mBackgroundPaint );
		}

		/*
		Set alpha and blur based on the elevation
		 */
		mShadowPaint.setColor( mShadowColor );
		mShadowPaint.setAlpha( (int) (60 + 190 * (mnObjectElevation / MAX_ELEVATION_LEVELS)));
		mShadowPaint.setMaskFilter( new BlurMaskFilter( mnShadowRadius, BlurMaskFilter.Blur.NORMAL ) );

		/*
		Calculate the offsets based on the elevation (at least 20% from the "ground").
		The less the elevation is, the smaller the offset is since the object is closer to the "ground"
		Also include the shadow radius in the offset so that we don't have to think about it in later calculations
		 */
		mnHorizontalOffset = (mbIsHorizontalOffsetRequired) ? (int)(((20 * (1 - mnObjectElevation / MAX_ELEVATION_LEVELS))) + (mnShadowRadius * 2)) : 0;
		mnVerticalOffset = (mbIsVerticalOffsetRequired) ? (int)(((20 * (1 - mnObjectElevation / MAX_ELEVATION_LEVELS))) + (mnShadowRadius * 2)) : 0;

		mBackgroundPaint.setColor( mBackgroundColor );

		setOnClickListener( this );

	}

	/**
	 * Called when a view has been clicked.
	 *
	 * @param v The view that was clicked.
	 */
	@Override
	public void onClick( View v ) {
		if(mClickListener != null)
			mClickListener.onViewClick( getId() );
	}

	/**
	 * Set the view click listener
	 * @param listener
	 *      The click listener
	 *
	 * @author Melvin Lobo
	 */
	public void setViewClickListener(ViewClickListener listener) {
		mClickListener = listener;
	}

	/**
	 * Load the Bitmap
	 *
	 * @author Melvin Lobo
	 */
	private void loadBitmap() {
		if(mDrawable == getDrawable())
			return;

		mDrawable = getDrawable();
		mBitmap = getBitmapFromDrawable();
		refreshShader();
	}

	/**
	 * Update the shader with the given bitmap. Note, to avoid clamping artifacts, make sure that the
	 * bitmap has transparency on at least the last pixel of its edges.
	 *
	 * @author Melvin Lobo
	 */
	private void refreshShader() {
		/*
		Handle the null bitmap case
		 */
		if(mBitmap == null)
			return;

		/*
		Create the bitmap shader and offer it as a source to the paint
		 */
		mBitmapShader = new BitmapShader( (mbShouldResizeBitmap) ? getScaledBitmap() : mBitmap, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP );

		/*
		 Apply translation to the shader matrix so that the resultant source pixels of the bitmap are centered
		 */
		Matrix shaderMatrix = new Matrix();
		float translateFactor = getTranslateFactor();
		shaderMatrix.postTranslate( translateFactor, translateFactor );
		mBitmapShader.setLocalMatrix( shaderMatrix );

		/*
		Set the shader as the source to the paint
		 */
		mBitmapPaint.setShader( mBitmapShader );
	}

	/**
	 * Create a bitmap from the drawable
	 * Creating a bitmap is faster than loading one. So, we'll create one:
	 * http://stackoverflow.com/questions/21895263/speed-comparison-decoderesource-vs-createbitmap
	 *
	 * @author Melvin Lobo
	 */
	private Bitmap getBitmapFromDrawable() {
		/*
		Return obvious cases
		 */
		if (mDrawable == null) {
			return null;
		} else if (mDrawable instanceof BitmapDrawable ) {
			return ((BitmapDrawable) mDrawable).getBitmap();
		}

		/*
		Check for valid sizes
		 */
		int nWidth = mDrawable.getIntrinsicWidth();
		int nHeight = mDrawable.getIntrinsicHeight();

		if((nWidth <= 0) && (nHeight <= 0))
			return null;

		/*
		Create a new bitmap and assign the drawable to it
		 */
		try {
			Canvas canvas = new Canvas();
			Bitmap bitmap = Bitmap.createBitmap( nWidth, nHeight, Bitmap.Config.ARGB_8888 );
			mDrawable.setBounds( 0, 0, nWidth, nHeight );
			mDrawable.draw( canvas );
			return bitmap;
		}
		catch ( OutOfMemoryError e ) {
			//Out of memory. Nothing we can do
			Log.d("SpiderMenu", "Out of memory exception when creating bitmap from drawable");
			return null;
		}

	}

	/**
	 * Scale the bitmap with Matrix scaling. We wil apply the translate factor later to the Bitmapshader
	 * that this bitmap needs to be a source to, since translation here will not work as the Shader will
	 * always use pixels from the left / top and the resultant image will be pinned to left/top.
	 *
	 * @author Melvin Lobo
	 */
	private Bitmap getScaledBitmap() {
		/*
		Create a matrix with the desired scaling and translation properties
		 */
		Matrix shaderMatrix = new Matrix(  );
		float nScaleFactor = getScaleFactor();
		shaderMatrix.postScale( nScaleFactor, nScaleFactor );

		/*
		Create a scaled down bitmap with the matrix. Since the scaled down version reference is with respect to the
		original bitmap #mBitmap, we use that size to create the scaled down bitmap so that scaling and translation
		can be applied correctly
		 */
		Bitmap scaledBitmap = Bitmap.createBitmap( mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), shaderMatrix, true );

		return scaledBitmap;
	}

	/**
	 * Get the scale factor of the drawable vs the drawable area.
	 * @return
	 *      The scale factor based on the smallest side
	 *
	 * @author Melvin Lobo
	 */
	private float getScaleFactor() {
		/*
		Drawable dimensions
		 */
		int nDrawableWidth = mBitmap.getWidth();
		int nDrawableHeight = mBitmap.getHeight();
		long nCanvasSize = getInsetSquareForBitmapDiameter();

		if(nCanvasSize * nDrawableHeight > nCanvasSize * nDrawableWidth) {
			return (float)nCanvasSize / (float)nDrawableHeight;        //Scale width
		}
		else {
			return (float)nCanvasSize / (float)nDrawableWidth;         //Scale height
		}
	}

	/**
	 * Get the translate factor based on teh scaling of the bitmap that has happened
	 * @return
	 *      The translate factor
	 *
	 * @author Melvin Lobo
	 */
	private float getTranslateFactor() {
		if(mbShouldResizeBitmap)
			return ((mnBitmapDiameter - getInsetSquareForBitmapDiameter()) / 2) + 0.5f;
		else {
			int nDrawableSize = Math.min(mDrawable.getIntrinsicHeight(), mDrawable.getIntrinsicWidth());
			return ((mnBitmapDiameter - nDrawableSize) / 2) + 0.5f;
		}
	}


	/**
	 * We have to remember that
	 * scaling the bitmap to the size of the canvas is not enough. The reason is that we have to
	 * draw the image with a circular shader, so the entire image has to fit within that circle.
	 * The mathematical formula for the largest square that fits in the circle of diameter mnBitmapDiameter is:
	 * 1. Diagonal = mnBitmapDiameter
	 * 2. Sides of the square are equal.
	 * 3. Pythagoras : a^2 + b^2 = c^2 ( hypotenuse )
	 * 4. a = b since sides of a square are equal. So a^2 + a^2 = c^2 -> 2a^2 = c^2
	 * 5. We already know c. Its the diagonal = diameter of circle = mnBitmapDiameter
	 * 6. Math.sqrt(2) * a = mnBitmapDiameter
	 * 7. Therefore, a = mnBitmapDiameter / Math.sqrt(2)
	 * @return
	 *      The largest square side that fits in a circle of diameter mnDiameter
	 *
	 * @author Melvin Lobo
	 */
	private long getInsetSquareForBitmapDiameter() {
		return Math.round( (float)mnBitmapDiameter / Math.sqrt( 2 ) );
	}

	/**
	 * Handle onMeasure to measure a square based on the smallest value (Width or height).
	 * The reason is that the circle will fit perfectly in a square and we don't have to worry about
	 * center calculations. Also account for the horizontal and vertical offsets and then adjust
	 * the bitmap diameter with the largest offsets so that it fits in the optimal value
	 *
	 * @param widthMeasureSpec
	 *      The width measure spec
	 * @param heightMeasureSpec
	 *      The height measure spec
	 *
	 * @author Melvin Lobo
	 */
	@Override
	protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
		if(getDrawable() == null) {
			super.onMeasure( widthMeasureSpec, heightMeasureSpec );
		}
		else {
			int nWidth = MeasureSpec.getSize( widthMeasureSpec );
			int nHeight = MeasureSpec.getSize( heightMeasureSpec );
			mnBitmapDiameter = Math.min( nWidth, nHeight );
			int nLargestOffset = Math.max( mnHorizontalOffset, mnVerticalOffset );      //Take the max offset as we have to work with s square
			int nOptimalX = getOptimalValue( mnBitmapDiameter + mnHorizontalOffset, nWidth, MeasureSpec.getMode( widthMeasureSpec ));
			int nOptimalY = getOptimalValue( mnBitmapDiameter + mnVerticalOffset, nHeight, MeasureSpec.getMode( heightMeasureSpec ) );
			int nSmallestOptimalSize = Math.min(nOptimalX, nOptimalY);                  //Again, smaller value for a fitting square

			/*
			Finally, we got our size, so calculate the diameter considering the largest offset,
			so that the shadow after the offset fits
			 */
			mnBitmapDiameter = nSmallestOptimalSize - nLargestOffset;

			setMeasuredDimension( nSmallestOptimalSize, nSmallestOptimalSize );
		}
	}

	/**
	 * Get the optimum desired values based on the Mode recommendation during onMeasure
	 *
	 * @param nDesiredValue
	 *            The desired value that we calculated
	 * @param nRecommendedValue
	 *            The recommended value that is passed in an onMeasure Pass
	 * @param nMode
	 *            The Mode that is passed in onMeasure Pass
	 *
	 * @return The optimal value to be used based on the Mode
	 */
	private int getOptimalValue(int nDesiredValue, int nRecommendedValue, int nMode) {
		int nFinalSize;

		switch (nMode) {
			case MeasureSpec.EXACTLY:
				nFinalSize = nRecommendedValue; // No Choice
				break;
			case MeasureSpec.AT_MOST:
				nFinalSize = Math.min(nDesiredValue, nRecommendedValue);
				break;
			default: // MeasureSpec.UNSPECIFIED
				nFinalSize = nDesiredValue;
				break;
		}

		return nFinalSize;
	}

	/**
	 * Draw the circular bitmap
	 * @param canvas
	 *      The canvas to draw on
	 *
	 * @author Melvin Lobo
	 */
	@Override
	protected void onDraw( Canvas canvas ) {

		/*
		Load the bitmap first
		 */
		loadBitmap();

		/*
		Draw the bitmap shader and the shadow.
		 */
		if(mBitmap != null) {
			int nRadius = mnBitmapDiameter / 2;
			int nCenterX = ( canvas.getWidth() - (mnBitmapDiameter + mnHorizontalOffset)) / 2;
			int nCenterY = ( canvas.getHeight() - (mnBitmapDiameter + mnVerticalOffset)) / 2;
			canvas.translate( nCenterX, nCenterY );
			canvas.drawCircle( nRadius + mnHorizontalOffset - ((mbIsHorizontalOffsetRequired) ? mnShadowRadius : 0), nRadius + mnVerticalOffset - ((mbIsVerticalOffsetRequired) ? mnShadowRadius : 0), nRadius, mShadowPaint );

			if(mBackgroundPaint.getColor() != Color.TRANSPARENT)
				canvas.drawCircle( nRadius, nRadius, nRadius, mBackgroundPaint );

			canvas.drawCircle( nRadius, nRadius, nRadius, mBitmapPaint );

		}
	}

	//////////////////////////////////////////// INTERFACES ////////////////////////////////////////

	/**
	 * On Click Listener
	 *
	 * @author Melvin Lobo
	 */
	public interface ViewClickListener {
		/**
		 * On View click
		 *
		 * @param nID
		 *      The View id
		 *
		 * @author Melvin Lobo
		 */
		void onViewClick(int nID);
	}
}
