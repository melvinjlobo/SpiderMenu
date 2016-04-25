package com.abysmel.spidermenu;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;

import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringListener;
import com.facebook.rebound.SpringSystem;
import com.facebook.rebound.SpringUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by Melvin Lobo on 4/18/2016.
 *
 * Radial Menu to display options in a radial. Children are
 * {@link com.abysmel.spidermenu.RoundedShadowImageView}
 *
 * The option when selected will move to the center of the menu and is used as a "back" button
 * to traverse in the reverse order
 *
 * FORMULAS:
 *
 * 1. The larger circle around which we draw the smaller circles has a total angle of 360 degrees
 * 2. We can divide the current item by the number of items we need to show so that they are equidistant from each other
 *    and then use that factor E.g. for three items the factors are
 *    1. 360 * (1/3) = 120
 *    2. 360 * (2/3) = 240
 *    3. 360 * (3/3) = 360
 * 3. The line equation (x,y) for a slant line along an angle "@" from center (x0, y0)is:
 *
 *    X Position = Start Point + Length of the Line * cos( Angle that the line has to be drawn on)
 *    Y Position = Start Point + Length of the Line * sin( Angle that the line has to be drawn on)
 *          OR
 *    x = x0 + r * cos(@)
 *    y = y0 + r * sin(@)
 *
 *
 * The formula for calculation of radius of smaller circles to draw around the outer circumference
 * of a larger circle is this:
 *  r = R sin(180/n) / (1- sin(180/n)) or n = 180 / arcsin(r/R+r)
 *  [http://mathforum.org/library/drmath/view/63736.html. The derivation is very easy to understand]
 *  where:
 *  r = Radius of smaller circles to be drawn
 *  R = Radius of the larger circle around whose circumference the smaller circles are to be drawn
 *  n = number of circles to be drawn
 *
 *  In our case
 *  R = Width / height of the view (we ensure its a square in onMeasure)
 *  n = Size of the sub menu list
 *
 *  We will use the above formula to figure out if the number of items are fitting with a given default
 *  radius. If not, we'll have to drop the radius length of the smaller circles using the same formula
 *
 *
 * Uses Facebook Rebound for interpolators
 * http://facebook.github.io/rebound/
 */
public class SpiderMenu extends ViewGroup implements RoundedShadowImageView.ViewClickListener {
	////////////////////////////////////// CLASS MEMBERS ///////////////////////////////////////////
	/**
	 * Static Definitions
	 */
	private static final int DEFAULT_MENUITEM_RADIUS = 70;
	private static final int DEFAULT_CENTER_MENU_RADIUS = 100;
	private static final double FULL_CIRCLE_ANGLE = 360;
	private static final double RADIAL_SCALE_TENSION = 200;
	private static final double RADIAL_SCALE_FRICTION = 10;
	private static final double CENTER_SCALE_TENSION = 400;
	private static final double CENTER_SCALE_FRICTION = 10;

	/**
	 * The menu item children count
	 */
	private int mnRadialChildCount = 0;

	/**
	 * The radius of the center Menu
	 */
	private int mnCenterMenuRadius = (int)Util.d2x( getContext(), DEFAULT_CENTER_MENU_RADIUS );

	/**
	 * The canvas size as a square
	 */
	private int mnCanvasSize = 0;

	/**
	 * The virtual circumference around which to place the sub menu items
	 */
	private int mnVirtualCircumferenceRadius = 0;

	/**
	 * The current radius of the menu item
	 */
	private int mnCurrentMenuItemRadius = (int)Util.d2x( getContext(), DEFAULT_MENUITEM_RADIUS );

	/**
	 * The animation transition value for the distance between the center circle item and the radial items
	 */
	private float mnDistanceTransition = 0.0f;

	/**
	 * Add a spring system
	 */
	private SpringSystem mSpringSystem = SpringSystem.create();

	/**
	 * ArrayList to sequentially run the radial animations
	 */
	private ArrayList<View> mAnimationQueue = new ArrayList<>(  );

	/**
	 * The center view. We need it during the animations
	 */
	private WeakReference<View> mCenterView = null;

	/**
	 * The click listener
	 */
	private SpiderMenuClickListener mSpiderMenuClickListener = null;

	////////////////////////////////////// CLASS METHODS ///////////////////////////////////////////
	/**
	 * Constructors
	 */
	public SpiderMenu( Context context ) {
		super( context );
	}

	public SpiderMenu( Context context, AttributeSet attrs ) {
		this( context, attrs, 0 );
	}

	public SpiderMenu( Context context, AttributeSet attrs, int defStyleAttr ) {
		super( context, attrs, defStyleAttr );
		initializeMenu(attrs);
	}

	@TargetApi( Build.VERSION_CODES.LOLLIPOP )
	public SpiderMenu( Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes ) {
		super( context, attrs, defStyleAttr, defStyleRes );
		initializeMenu(attrs);
	}

	/**
	 * Initialize the View
	 *
	 * @author Melvin Lobo
	 */
	private void initializeMenu( AttributeSet attrs ) {
		if(attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.SpiderMenu, 0, 0);
			mnCurrentMenuItemRadius = a.getDimensionPixelOffset( R.styleable.SpiderMenu_radialMenuRadiusSize, DEFAULT_MENUITEM_RADIUS );
			mnCenterMenuRadius = a.getDimensionPixelOffset( R.styleable.SpiderMenu_centerMenuRadiusSize, DEFAULT_CENTER_MENU_RADIUS );

			a.recycle();
		}

		/*
		Add a predraw listener to start animating the children just before they are drawn
		 */
		ViewTreeObserver observer = getViewTreeObserver();
		if((observer != null) && (observer.isAlive())) {
			observer.addOnPreDrawListener( new PreDrawListener() );
		}
		else {
			Logger.d( "Oops! No Predraw lIstener" );
		}

	}

	/**
	 * Set the click listener
	 *
	 * @author Melvin Lobo
	 */
	public void setSpiderMenuClickListener(SpiderMenuClickListener listener) {
			mSpiderMenuClickListener = listener;
	}

	/**
	 * We do not scroll
	 * @return
	 *      false
	 *
	 * @author Melvin Lobo
	 */
	@Override
	public boolean shouldDelayChildPressedState() {
		return false;
	}

	/**
	 * Measure the view optimally and then the children accordingly as per the given radii.
	 * The important part here is to measure the children and set their radii.
	 *
	 * @param widthMeasureSpec
	 *      The width MeasureSpec
	 * @param heightMeasureSpec
	 *      The height MeasureSpec
	 *
	 * @author Melvin Lobo
	 */
	@Override
	protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
		int nWidth = MeasureSpec.getSize( widthMeasureSpec );
		int nHeight = MeasureSpec.getSize( heightMeasureSpec );

		/*
		Get child count if not already done. Only Radial items
		 */
		if( mnRadialChildCount == 0) {
			int nChildCount = 0;
			for(int nCtr = 0; nCtr < getChildCount(); ++nCtr) {
				View view = getChildAt( nCtr );
				if(((SpiderMenu.LayoutParams)view.getLayoutParams()).getMenuType() == LayoutParams.RADIAL)
					nChildCount++;
			}
			mnRadialChildCount = nChildCount;
		}

		/*
		Pick the smaller size to make it a square
		 */
		mnCanvasSize = Math.min( nHeight, nWidth );

		/*
		Initialize default value for virtual Circumference the first time
		 */
		if(mnVirtualCircumferenceRadius == 0) {
			mnVirtualCircumferenceRadius = (mnCanvasSize / 2) - (mnCurrentMenuItemRadius * 2);
		}

		/*
		Get the radii for the Radial, canter and virtual circumference along which the menu item circles will be drawn
		along
		 */
		calculateRadialRadii();
		calculateCenterRadii();

		int childSpecRadial = MeasureSpec.makeMeasureSpec( mnCurrentMenuItemRadius * 2, MeasureSpec.EXACTLY );
		int childSpecCenter = MeasureSpec.makeMeasureSpec( mnCenterMenuRadius * 2, MeasureSpec.EXACTLY );

		/*
		Measure children
		 */
		for( int nCtr = 0; nCtr < getChildCount(); ++nCtr) {
			View child = getChildAt( nCtr );
			if(((SpiderMenu.LayoutParams)child.getLayoutParams()).getMenuType() == LayoutParams.RADIAL ) {
				child.measure( childSpecRadial, childSpecRadial );
			}
			else {
				/*
				Use this opportunity to bring the center child above all other children so that
				we can use the animation effectively
				 */
				bringChildToFront( child );
				/*
				Hold on to the center child
				 */
				if(mCenterView == null)
					mCenterView = new WeakReference<>( child );

				child.measure( childSpecCenter, childSpecCenter );
			}
		}

		setMeasuredDimension( getOptimalValue( mnCanvasSize, nWidth, MeasureSpec.getMode( widthMeasureSpec ) ),
				getOptimalValue( mnCanvasSize, nHeight, MeasureSpec.getMode( heightMeasureSpec ) ) );
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
	 * Layout the children based on the above formula for placing the circles on lines with angle @
	 * @param changed
	 *      New size or position
	 * @param leftRelativeToParent
	 *      Left, w.r.t Parent
	 * @param topRelativeToParent
 *          Top, w.r.t Parent
	 * @param rightRelativeToParent
	 *      Right, w.r.t Parent
	 * @param bottomRelativeToParent
	 *      Bottom, w.r.t Parent
	 *
	 * @author Melvin Lobo
	 */
	@Override
	protected void onLayout( boolean changed, int leftRelativeToParent, int topRelativeToParent, int rightRelativeToParent, int bottomRelativeToParent ) {
		int nCenter = mnCanvasSize / 2;
		float nAngleForOneCircle = ((float) FULL_CIRCLE_ANGLE / (float) mnRadialChildCount);
		double nStartAngle = Math.toRadians( 270 );       //Start from Top (positive Y)
		int nChildCenter = mnVirtualCircumferenceRadius + mnCurrentMenuItemRadius;

		for( int nCtr = 0; nCtr < getChildCount(); ++nCtr) {
			View child = getChildAt( nCtr );
			if(((SpiderMenu.LayoutParams)child.getLayoutParams()).getMenuType() == LayoutParams.RADIAL ) {
				double angle = nStartAngle + Math.toRadians( nAngleForOneCircle * nCtr );

				/*
				Center point of the menuitem circle
				 */
				int nX = (int) ((nCenter) + nChildCenter * Math.cos( angle ));
				int nY = (int) ((nCenter) + nChildCenter * Math.sin( angle ));

				child.layout( nX - mnCurrentMenuItemRadius, nY - mnCurrentMenuItemRadius, nX + mnCurrentMenuItemRadius, nY + mnCurrentMenuItemRadius );
			}
			else {
				child.layout( nCenter - mnCenterMenuRadius, nCenter - mnCenterMenuRadius, nCenter + mnCenterMenuRadius, nCenter + mnCenterMenuRadius );
			}
		}
	}

	/**
	 * Get the default radius of the menu item circles to fit on the circumference of the virtual circle.
	 * If they do not fit, or if the virtual circle is smaller than the surrounding menu item circles,
	 * recalculate both radii using the formula described in the file javadoc, to find a smaller radius
	 *
	 * @return
	 *      The radius that fits amicably in the view
	 * @author Melvin Lobo
	 */
	private void calculateRadialRadii() {
		/*
		In case the radius is greater than the virtual circle circumference
		 */
		if(mnCurrentMenuItemRadius > mnVirtualCircumferenceRadius) {
			mnCurrentMenuItemRadius = getAppropriateRadius();
		}
		/*
		The radius is smaller. Now check if the number of items can fit with this radius (along with
		a gap of at least one circle (+ 1) to ensure some space between the menu item circles)
		 */
		else {
			/*
			The number of menu item circles that can fit on the circumference of the Virtual circle
			{ n = 180 / sin(r / R + r) }
			 */
			long nNumberOfCircles = Math.round(Math.toRadians( 180 ) / Math.asin((double)mnCurrentMenuItemRadius / (double)(mnVirtualCircumferenceRadius + mnCurrentMenuItemRadius)));

			/*
			If the number of circles that can be fit is equal to or greater than the menu item circles
			of the given radius + equidistant empty spaces (equal to another circle), we are fine, else,
			we will have to reduce the radius
			 */
			if(nNumberOfCircles < ( mnRadialChildCount + 1 )) {
				mnCurrentMenuItemRadius = getAppropriateRadius();
			}
		}

		/*
		Reduce virtual circumference by the smaller diameter to fit the smaller circles on the canvas
		 */
		mnVirtualCircumferenceRadius = (mnCanvasSize / 2) - (mnCurrentMenuItemRadius * 2);
	}

	/**
	 * Calculate the radius of the center menu item
	 *
	 * @author Melvin Lobo
	 */
	private void calculateCenterRadii() {
		mnCenterMenuRadius = Math.min( DEFAULT_CENTER_MENU_RADIUS, mnVirtualCircumferenceRadius );
	}

	/**
	 * Get the right radius using trigonometry to fit in the circles around the circumference
	 *
	 * @return
	 *      The calculated radius of the orbitting circles
	 *
	 * @author Melvin Lobo
	 */
	private int getAppropriateRadius() {
		/*
		r = R * sin ( 180 / n ) / ( 1 - sin( 180 / n))
		 */
		int numberOfItems = mnRadialChildCount + 1;       //+ 1 for some spacing between the menu item circles
		return (int)Math.abs((mnVirtualCircumferenceRadius * Math.sin(Math.toRadians( 180 ) / numberOfItems) / (1 - Math.sin(Math.toRadians( 180 ) / numberOfItems))));
	}

	/**
	 * Start Center Animation
	 *
	 * @author Melvin Lobo
	 */
	public void startCenterAnimation() {
		/*
		Scale down all the surrounding radial views till the center animation is done.
		Also dump all radial children into a list for sequential animation after the
		center animation is done.
		 */
		for(int nCtr = 0; nCtr < getChildCount(); ++nCtr) {
			final View child = getChildAt( nCtr );
			LayoutParams params = (LayoutParams) child.getLayoutParams();
			if ( params.getMenuType() != LayoutParams.CENTER ) {
				/*
				Reduce the scale initially till we are done with the center animation
				 */
				ViewCompat.setScaleX( child, 0 );
				ViewCompat.setScaleY( child, 0 );
				mAnimationQueue.add( child );
			}
		}

		/*
		 Set the initial scale to zero
		 */
		ViewCompat.setScaleX( mCenterView.get(), 0 );
		ViewCompat.setScaleY( mCenterView.get(), 0 );

		Handler handler = new Handler(  );
		handler.postDelayed( new Runnable() {
			@Override
			public void run() {
				/*
				Bounce the center view.
				 */
				bounceView( mCenterView.get(), new SpringAnimator( mCenterView.get(), new SpringEndListener() {
					@Override
					public void onSpringRested() {
						startRadialOutAnimationForNext( null );
					}
				} ) );

			}
		}, 1000 );
	}

	/**
	 * Start Radial animation
	 *
	 * @param previousChildDoneWithAnimation
	 *      Previous child which has completed the animation
	 *
	 * @author Melvin Lobo
	 */
	public void startRadialOutAnimationForNext( View previousChildDoneWithAnimation) {

		if(previousChildDoneWithAnimation != null)
			mAnimationQueue.remove( previousChildDoneWithAnimation );

		if(mAnimationQueue.isEmpty())
			return;

		/*
		Get the child and its layout params to figure out if its a radial item or the center item
		 */
		final View child = mAnimationQueue.get( 0 );
		springOutRadialChild( child );
		bounceView( mCenterView.get(), new SpringAnimator( mCenterView.get(), null ) );
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		for(int nCtr = 0; nCtr < getChildCount(); ++nCtr) {
			View view = getChildAt( nCtr );
			if(!(view instanceof RoundedShadowImageView))
				throw  new IllegalArgumentException( "The children should be of type RoundedShadowImageView" );

			((RoundedShadowImageView)view).setViewClickListener( this );
		}
	}

	/**
	 * Handles the bounce out animation for the radial menu items
	 * @param child
	 *      The child which needs to be animated
	 *
	 * @author Melvin Lobo
	 */
	private void springOutRadialChild( final View child) {
		/*
		Get the center of the View group
		 */
		int nCenter = mnCanvasSize / 2;

		/*
		For Radial items, start the animation. Calculate the distance to align
		the center of the Viewgroup and View and animate from there:
		For X - Center - child left - half of width(to align center)
		For Y - Center - child left - half of height(to align center)
		 */
		ViewCompat.setTranslationX( child, ( nCenter - child.getLeft() - (child.getWidth() / 2) ));
		ViewCompat.setTranslationY( child, ( nCenter - child.getTop() - (child.getHeight() / 2) ));
		ViewCompat.setAlpha( child, 0 );
		ViewCompat.setScaleX( child, 1 );       //Ensure the scale is one, because it might have been set to zero during the initial animation
		ViewCompat.setScaleY( child, 1 );       //Ensure the scale is one, because it might have been set to zero during the initial animation
		ViewCompat.animate( child )
				.translationX( 0 )
				.translationY( 0 )
				.alpha( 1 )
				.setInterpolator( new DecelerateInterpolator() )
				.setDuration( 350 )
				.withLayer()
				.withEndAction( new Runnable() {
					@Override
					public void run() {
						/*ViewCompat.setScaleX( child, 0.6f );
						ViewCompat.setScaleY( child, 0.6f );*/
						startRadialOutAnimationForNext( child );
						bounceView( child, new SpringAnimator( child, null) );
					}
				} );
	}

	/**
	 * Close animation by consolidating the children
	 *
	 * @author Melvin Lobo
	 */
	private void springInChildren() {

		/*
		Get the center of the View group
		 */
		int nCenter = mnCanvasSize / 2;

		for(int nCtr = 0; nCtr < getChildCount(); ++nCtr) {
			final View child = getChildAt( nCtr );

			LayoutParams params = (LayoutParams) child.getLayoutParams();
			if ( params.getMenuType() == LayoutParams.CENTER )
				continue;

			/*
			For Radial items, start the animation. Calculate the distance to align
			the center of the Viewgroup and View and animate from there:
			For X - Center - child left - half of width(to align center)
			For Y - Center - child left - half of height(to align center)
			 */
			ViewCompat.setAlpha( child, 1 );
			ViewCompat.setScaleX( child, 1 );       //Ensure the scale is one, because it might have been set to zero during the initial animation
			ViewCompat.setScaleY( child, 1 );       //Ensure the scale is one, because it might have been set to zero during the initial animation
			ViewCompat.animate( child )
					.translationX( ( nCenter - child.getLeft() - (child.getWidth() / 2) ) )
					.translationY( ( nCenter - child.getTop() - (child.getHeight() / 2) ) )
					.alpha( 0 )
					.scaleX( 0 )
					.scaleY( 0 )
					.setInterpolator( new DecelerateInterpolator() )
					.setDuration( 250 )
					.withLayer()
					.withEndAction( new Runnable() {
						@Override
						public void run() {
							bounceView( mCenterView.get(), new SpringAnimator( mCenterView.get(), new SpringEndListener() {
								@Override
								public void onSpringRested() {
									ViewCompat.animate( mCenterView.get() )
											  .scaleX( 0 )
											  .scaleY( 0 )
											  .withLayer();
								}
							} ) );
						}
					} );
		}
	}

	/**
	 * Bounce the view. Will use the respective values for center and radial view
	 * @param child
	 *      The child that has to be bounced
	 * @param springAnimator
	 *      The spring animator with the listener to be invoked.
	 *      Set null if you dont need to intercept the spring animation end
	 *
	 * @author Melvin Lobo
	 */
	private void bounceView(View child, SpringAnimator springAnimator) {
		LayoutParams params = (LayoutParams) child.getLayoutParams();

		if(params.getMenuType() == LayoutParams.CENTER) {
			Spring centerScaleSpring = mSpringSystem.createSpring();
			centerScaleSpring.setSpringConfig( new SpringConfig( CENTER_SCALE_TENSION, CENTER_SCALE_FRICTION ) );
			centerScaleSpring.setEndValue( 1 );
			centerScaleSpring.addListener( springAnimator );
		}
		else {
			Spring radialScaleSpring = mSpringSystem.createSpring();
			radialScaleSpring.setSpringConfig( new SpringConfig( RADIAL_SCALE_TENSION, RADIAL_SCALE_FRICTION ) );
			radialScaleSpring.setEndValue( 1 );
			radialScaleSpring.addListener( springAnimator );
		}
	}

	/**
	 * LayoutParams Overrides. Need them if we want the child to use our LayoutParams
	 *
	 * @author Melvin Lobo
	 */
	@Override
	protected boolean checkLayoutParams( ViewGroup.LayoutParams p ) {
		return p instanceof LayoutParams;
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams( super.generateDefaultLayoutParams() );
	}

	@Override
	public LayoutParams generateLayoutParams( AttributeSet attrs ) {
		return new LayoutParams( getContext(), attrs );
	}

	@Override
	protected ViewGroup.LayoutParams generateLayoutParams( ViewGroup.LayoutParams p ) {
		return new LayoutParams( p );
	}

	/**
	 * On View click
	 *
	 * @param nID The View id
	 * @author Melvin Lobo
	 */
	@Override
	public void onViewClick( int nID ) {
		if(mSpiderMenuClickListener != null)
			mSpiderMenuClickListener.onSpiderMenuClick( nID );
		springInChildren();
	}

	////////////////////////////////////// INNER CLASSES ///////////////////////////////////////////
	/**
	 * Custom Layout Params to provide additional information on the type of the menuitem
	 *
	 * @author Melvin Lobo
	 */
	public static class LayoutParams extends ViewGroup.LayoutParams {
		//////////////////////////////////// CLASS MEMBERS /////////////////////////////////////////
		/**
		 * Define the integers
		 *
		 * For simplicity from Android documentation -
		 * We create a new annotation (that's what
		 * the above line "public @interface MenuType {}" does) and then we annotate the
		 * annotation itself with @IntDef, and we specify the constants that are the valid constants
		 * for return values or parameters. We also add the line "@Retention(RetentionPolicy.SOURCE)"
		 * to tell the compiler that usages of the new typedef annotation do not need to be recorded
		 * in .class files.
		 */
		@IntDef({
				CENTER,
				RADIAL
		})
		@Retention(RetentionPolicy.SOURCE)
		@interface MenuType {}

		/**
		 * The view will be at the center of the menu surrounded by Radial Menu items
		 * {@link SpiderMenu}.
		 */
		public static final int CENTER = 0;

		/**
		 * The view will be a radial menu item option
		 * {@link SpiderMenu}.
		 */
		public static final int RADIAL = 1;

		/**
		 * The menu type
		 */
		private int mnMenuType = RADIAL;

		//////////////////////////////////// CLASS METHODS /////////////////////////////////////////

		/**
		 * Creates a new set of layout parameters. The values are extracted from
		 * the supplied attributes set and context. The XML attributes mapped
		 * to this set of layout parameters are:
		 * <p>
		 * <ul>
		 * <li><code>layout_width</code>: the width, either an exact value,
		 * {@link #WRAP_CONTENT}, or {@link #FILL_PARENT} (replaced by
		 * {@link #MATCH_PARENT} in API Level 8)</li>
		 * <li><code>layout_height</code>: the height, either an exact value,
		 * {@link #WRAP_CONTENT}, or {@link #FILL_PARENT} (replaced by
		 * {@link #MATCH_PARENT} in API Level 8)</li>
		 * </ul>
		 *
		 * @param c     the application environment
		 * @param attrs the set of attributes from which to extract the layout
		 */
		public LayoutParams( Context c, AttributeSet attrs ) {
			super( c, attrs );

			TypedArray a = c.obtainStyledAttributes(attrs,
					R.styleable.SpiderMenu);
			mnMenuType = a.getInt(R.styleable.SpiderMenu_menuType, RADIAL);

			a.recycle();
		}

		/**
		 * Creates a new set of layout parameters with the specified width
		 * and height.
		 *
		 * @param width  the width, either {@link #WRAP_CONTENT},
		 *               {@link #FILL_PARENT} (replaced by {@link #MATCH_PARENT} in
		 *               API Level 8), or a fixed size in pixels
		 * @param height the height, either {@link #WRAP_CONTENT},
		 *               {@link #FILL_PARENT} (replaced by {@link #MATCH_PARENT} in
		 */
		public LayoutParams( int width, int height ) {
			super( width, height );
		}

		public LayoutParams( int width, int height, int nMenuType ) {
			super( width, height );
			mnMenuType = nMenuType;
		}

		/**
		 * Copy constructor. Clones the width and height values of the source.
		 *
		 * @param source The layout params to copy from.
		 */
		public LayoutParams( ViewGroup.LayoutParams source ) {
			super( source );
		}

		/**
		 * Set the Menu Type.
		 *
		 * @param nMenuType one of {@link #CENTER} or {@link #RADIAL}
		 */
		public void setMenuType(@MenuType int nMenuType) {
			mnMenuType = nMenuType;
		}

		/**
		 * Returns the requested menu type.
		 *
		 * @return the Menu Type. One of {@link #CENTER} or {@link #RADIAL}
		 */
		@MenuType
		public int getMenuType() {
			return mnMenuType;
		}
	}

	/**
	 * Predraw listener to start animating the children. Neat place to start animations
	 * Thank You Chet Haase :D
	 *
	 * @author Melvin Lobo
	 */
	private class PreDrawListener implements ViewTreeObserver.OnPreDrawListener {
		@Override
		public boolean onPreDraw() {
			/*
			Remove the pre draw listener as we will do the animations only once
			 */
			getViewTreeObserver().removeOnPreDrawListener(this);
			Handler handler = new Handler();
			handler.post( new Runnable() {
				@Override
				public void run() {
					startCenterAnimation();
				}
			} );

			/*
			Return true so that the drawing can continue
			 */
			return true;
		}
	}

	/**
	 * Class which will take a view and do a spring animation on its scale
	 *
	 * @author Melvin Lobo
	 */
	private class SpringAnimator implements SpringListener {


		//////////////////////////////////// CLASS MEMBERS /////////////////////////////////////////
		private WeakReference<View> mView;

		private SpringEndListener mEndListener = null;

		//////////////////////////////////// CLASS METHODS /////////////////////////////////////////

		/**
		 * Constructor
		 * @param view
		 *      The view to animate
		 * @param endListener
		 *      Spring animation end listener
		 *
		 * @author Melvin Lobo
		 */
		public SpringAnimator(View view, SpringEndListener endListener) {
			mView = new WeakReference<View>( view );
			mEndListener = endListener;
		}

		/**
		 * Called whenever the spring is updated
		 *
		 * @param spring the Spring sending the update
		 */
		@Override
		public void onSpringUpdate( Spring spring ) {
			/*
			Map our current value to scale values. Rebound has a nifty SpringUtil class to do that
			We map our values between scale 0.3 to 1 (Range of 30% to 100%)
			 */
			double nMappedValue = SpringUtil.mapValueFromRangeToRange( spring.getCurrentValue(), 0.0d, 1.0d, 0.8d, 1.0d);
			mView.get().setScaleX( (float)nMappedValue );
			mView.get().setScaleY( (float)nMappedValue );
		}

		/**
		 * called whenever the spring achieves a resting state
		 *
		 * @param spring the spring that's now resting
		 */
		@Override
		public void onSpringAtRest( Spring spring ) {
			if(mEndListener != null)
				mEndListener.onSpringRested();
		}

		/**
		 * called whenever the spring leaves its resting state
		 *
		 * @param spring the spring that has left its resting state
		 */
		@Override
		public void onSpringActivate( Spring spring ) {

		}

		/**
		 * called whenever the spring notifies of displacement state changes
		 *
		 * @param spring the spring whose end state has changed
		 */
		@Override
		public void onSpringEndStateChange( Spring spring ) {

		}
	}

	/**
	 * Helper Class to define the attributes for the Menu Item to add programmatically rather than
	 * via an xml file
	 * @author Melvin Lobo
	 */
	public static class SpiderMenuItem {
		//////////////////////////////////// CLASS MEMBERS /////////////////////////////////////////
		/**
		 * The Drawable resource to be used to represent this menu item
		 */
		private int mnDrawableResourceID = 0;

		/**
		 * The menu item type
		 */
		private int mnMenuType = LayoutParams.RADIAL;

		/**
		 * The list of sub menus for this menu
		 */
		private ArrayList<SpiderMenuItem> mMenuItemList = null;

		//////////////////////////////////// CLASS METHODS /////////////////////////////////////////

		/**
		 * Getters and Setters
		 */
		public int getDrawableResourceID() {
			return mnDrawableResourceID;
		}

		public void setDrawableResourceID( int nDrawableResourceID ) {
			mnDrawableResourceID = nDrawableResourceID;
		}

		public ArrayList<SpiderMenuItem> getMenuItemList() {
			return mMenuItemList;
		}

		public void setMenuItemList( ArrayList<SpiderMenuItem> menuItemList ) {
			mMenuItemList = menuItemList;
		}

		public boolean hasSubMenu() {
			return ((mMenuItemList != null) && (!mMenuItemList.isEmpty()));
		}

		public int getMenuType() {
			return mnMenuType;
		}

		public void setMenuType( int nMenuType ) {
			mnMenuType = nMenuType;
		}

	}

	///////////////////////////////////////// INTERFACES ///////////////////////////////////////////
	/**
	 * Spring animation end notification listener
	 *
	 * @author Melvin Lobo
	 */
	private interface SpringEndListener {
		void onSpringRested();
	}

	/**
	 * Interface for click listener
	 *
	 * @author Melvin Lobo
	 */
	public interface SpiderMenuClickListener {
		void onSpiderMenuClick(int nID);
	}
}
