package eu.davidea.fastscroller;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.davidea.flexibleadapter.R;

/**
 * Class taken from GitHub, customized and optimized for FlexibleAdapter project.
 *
 * @see <a href="https://github.com/AndroidDeveloperLB/LollipopContactsRecyclerViewFastScroller">
 * github.com/AndroidDeveloperLB/LollipopContactsRecyclerViewFastScroller</a>
 * @since Up to the date 23/01/2016
 * <br/>23/01/2016 Added onFastScrollerStateChange in the listener
 */
public class FastScroller extends FrameLayout {

    private static final int BUBBLE_ANIMATION_DURATION = 300;
    private static final int TRACK_SNAP_RANGE = 5;

    private TextView bubble;
    private ImageView handle;
    private int height;
    private boolean isInitialized = false;
    private ObjectAnimator currentAnimator;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private BubbleTextCreator bubbleTextCreator;
    private List<ScrollStateChangeListener> scrollerListeners = new ArrayList<ScrollStateChangeListener>();

    private final RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (bubble == null || handle.isSelected())
                return;
            int verticalScrollOffset = recyclerView.computeVerticalScrollOffset();
            int verticalScrollRange = recyclerView.computeVerticalScrollRange();
            float proportion = (float) verticalScrollOffset / ((float) verticalScrollRange - height);
            setBubbleAndHandlePosition(height * proportion);
        }
    };

    public FastScroller(Context context) {
        super(context);
        init();
    }

    public FastScroller(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FastScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    protected void init() {
        if (isInitialized) return;
        isInitialized = true;
        setClipChildren(false);
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        this.recyclerView.addOnScrollListener(onScrollListener);
        this.layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

        if (recyclerView.getAdapter() instanceof BubbleTextCreator)
            this.bubbleTextCreator = (BubbleTextCreator) recyclerView.getAdapter();
        if (recyclerView.getAdapter() instanceof ScrollStateChangeListener)
            addScrollListener((ScrollStateChangeListener) recyclerView.getAdapter());

        this.recyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                FastScroller.this.recyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                if (bubble == null || handle.isSelected()) return true;
                int verticalScrollOffset = FastScroller.this.recyclerView.computeVerticalScrollOffset();
                int verticalScrollRange = FastScroller.this.computeVerticalScrollRange();
                float proportion = (float) verticalScrollOffset / ((float) verticalScrollRange - height);
                setBubbleAndHandlePosition(height * proportion);
                return true;
            }
        });
    }

    public void addScrollListener(ScrollStateChangeListener scrollerListener) {
        if (!scrollerListeners.contains(scrollerListener))
            scrollerListeners.add(scrollerListener);
    }

    private void notifyScrollStateChange(boolean scrolling) {
        for (ScrollStateChangeListener scrollerListener : scrollerListeners) {
            scrollerListener.onFastScrollerStateChange(scrolling);
        }
    }

    /**
     * Layout customization.<br/>
     * Color for Selected State is the color defined inside the Drawables.
     *
     * @param layoutResId Main layout of Fast Scroller
     * @param bubbleResId Drawable resource for Bubble containing the Text
     * @param handleResId Drawable resource for the Handle
     */
    public void setViewsToUse(@LayoutRes int layoutResId, @IdRes int bubbleResId, @IdRes int handleResId) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(layoutResId, this, true);
        bubble = (TextView) findViewById(bubbleResId);
        if (bubble != null) bubble.setVisibility(INVISIBLE);
        handle = (ImageView) findViewById(handleResId);
    }

    /**
     * Layout customization<br/>
     * Color for Selected State is also customized by the user.
     *
     * @param layoutResId Main layout of Fast Scroller
     * @param bubbleResId Drawable resource for Bubble containing the Text
     * @param handleResId Drawable resource for the Handle
     * @param accentColor Color for Selected state during touch and scrolling (usually accent color)
     */
    public void setViewsToUse(@LayoutRes int layoutResId, @IdRes int bubbleResId, @IdRes int handleResId, int accentColor) {
        setViewsToUse(layoutResId, bubbleResId, handleResId);
        setBubbleAndHandleColor(accentColor);
    }

    private void setBubbleAndHandleColor(int accentColor) {
        //TODO: Programmatically generate the Drawables instead of using resources
        //BubbleDrawable accentColor
        GradientDrawable bubbleDrawable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bubbleDrawable = (GradientDrawable) getResources().getDrawable(R.drawable.fast_scroller_bubble, null);
        } else {
            //noinspection deprecation
            bubbleDrawable = (GradientDrawable) getResources().getDrawable(R.drawable.fast_scroller_bubble);
        }
        assert bubbleDrawable != null;
        bubbleDrawable.setColor(accentColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            bubble.setBackground(bubbleDrawable);
        } else {
            //noinspection deprecation
            bubble.setBackgroundDrawable(bubbleDrawable);
        }

        //HandleDrawable accentColor
        try {
            StateListDrawable stateListDrawable;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                stateListDrawable = (StateListDrawable) getResources().getDrawable(R.drawable.fast_scroller_handle, null);
            } else {
                //noinspection deprecation
                stateListDrawable = (StateListDrawable) getResources().getDrawable(R.drawable.fast_scroller_handle);
            }
            //Method is still hidden, invoke Java reflection
            Method getStateDrawable = StateListDrawable.class.getMethod("getStateDrawable", int.class);
            GradientDrawable handleDrawable = (GradientDrawable) getStateDrawable.invoke(stateListDrawable, 0);
            handleDrawable.setColor(accentColor);
            handle.setImageDrawable(stateListDrawable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        height = h;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (event.getX() < handle.getX() - ViewCompat.getPaddingStart(handle)) return false;
                if (currentAnimator != null) currentAnimator.cancel();
                handle.setSelected(true);
                notifyScrollStateChange(true);
                showBubble();
            case MotionEvent.ACTION_MOVE:
                float y = event.getY();
                setBubbleAndHandlePosition(y);
                setRecyclerViewPosition(y);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handle.setSelected(false);
                notifyScrollStateChange(false);
                hideBubble();
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (recyclerView != null)
            recyclerView.removeOnScrollListener(onScrollListener);
    }

    private void setRecyclerViewPosition(float y) {
        if (recyclerView != null) {
            int itemCount = recyclerView.getAdapter().getItemCount();
            float proportion;
            if (handle.getY() == 0)
                proportion = 0f;
            else if (handle.getY() + handle.getHeight() >= height - TRACK_SNAP_RANGE)
                proportion = 1f;
            else
                proportion = y / (float) height;
            int targetPos = getValueInRange(0, itemCount - 1, (int) (proportion * (float) itemCount));
            //String bubbleText = bubbleTextCreator.onCreateBubbleText(targetPos);
            //String bubbleText = bubbleTextCreator.onCreateBubbleText(itemCount);
            String bubbleText = getBubbleTextString(targetPos);
            layoutManager.scrollToPositionWithOffset(targetPos, 0);
            if (bubble != null)
                bubble.setText(bubbleText);
        }
    }

    private int getValueInRange(int min, int max, int value) {
        int minimum = Math.max(min, value);
        return Math.min(minimum, max);
    }

    private void setBubbleAndHandlePosition(float y) {
        int handleHeight = handle.getHeight();
        handle.setY(getValueInRange(0, height - handleHeight, (int) (y - handleHeight / 2)));
        if (bubble != null) {
            int bubbleHeight = bubble.getHeight();
            bubble.setY(getValueInRange(0, height - bubbleHeight - handleHeight / 2, (int) (y - bubbleHeight)));
        }
    }

    private void showBubble() {
        if (bubble != null && bubble.getVisibility() != VISIBLE) {
            bubble.setVisibility(VISIBLE);
            if (currentAnimator != null)
                currentAnimator.cancel();
            currentAnimator = ObjectAnimator.ofFloat(bubble, "alpha", 0f, 1f).setDuration(BUBBLE_ANIMATION_DURATION);
            currentAnimator.start();
        }
    }

    private void hideBubble() {
        if (bubble == null)
            return;
        if (currentAnimator != null)
            currentAnimator.cancel();
        currentAnimator = ObjectAnimator.ofFloat(bubble, "alpha", 1f, 0f).setDuration(BUBBLE_ANIMATION_DURATION);
        currentAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                bubble.setVisibility(INVISIBLE);
                currentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                bubble.setVisibility(INVISIBLE);
                currentAnimator = null;
            }
        });
        currentAnimator.start();
    }

    public interface BubbleTextCreator {
        String onCreateBubbleText(int pos);
    }

    public interface ScrollStateChangeListener {
        void onFastScrollerStateChange(boolean scrolling);
    }

    public String getBubbleTextString(int target) {
        String bubbleTextString = "A";
        //String [] temp = getResources().getStringArray(R.array.animals);
        String[] temp = getResources().getStringArray(R.array.animals);
        int length = temp.length;
        Arrays.sort(temp);
        //String [] aContacts = new String[length];
        List<String> aContacts = new ArrayList<String>();
        List<String> bContacts = new ArrayList<String>();
        List<String> cContacts = new ArrayList<String>();
        List<String> dContacts = new ArrayList<String>();
        List<String> eContacts = new ArrayList<String>();
        List<String> fContacts = new ArrayList<String>();
        List<String> gContacts = new ArrayList<String>();
        List<String> hContacts = new ArrayList<String>();
        List<String> iContacts = new ArrayList<String>();
        List<String> jContacts = new ArrayList<String>();
        List<String> kContacts = new ArrayList<String>();
        List<String> lContacts = new ArrayList<String>();
        List<String> mContacts = new ArrayList<String>();
        List<String> nContacts = new ArrayList<String>();
        List<String> oContacts = new ArrayList<String>();
        List<String> pContacts = new ArrayList<String>();
        List<String> qContacts = new ArrayList<String>();
        List<String> rContacts = new ArrayList<String>();
        List<String> sContacts = new ArrayList<String>();
        List<String> tContacts = new ArrayList<String>();
        List<String> uContacts = new ArrayList<String>();
        List<String> vContacts = new ArrayList<String>();
        List<String> wContacts = new ArrayList<String>();
        List<String> xContacts = new ArrayList<String>();
        List<String> yContacts = new ArrayList<String>();
        List<String> zContacts = new ArrayList<String>();

        int i = 0;
        for (i = 0; i < length; i++) {
            if (temp[i].startsWith("A")) {
                aContacts.add(temp[i]);
            } else if (temp[i].startsWith("B")) {
                bContacts.add(temp[i]);
            } else if (temp[i].startsWith("C")) {
                cContacts.add(temp[i]);
            } else if (temp[i].startsWith("D")) {
                dContacts.add(temp[i]);
            } else if (temp[i].startsWith("E")) {
                eContacts.add(temp[i]);
            } else if (temp[i].startsWith("F")) {
                fContacts.add(temp[i]);
            } else if (temp[i].startsWith("G")) {
                gContacts.add(temp[i]);
            } else if (temp[i].startsWith("H")) {
                hContacts.add(temp[i]);
            } else if (temp[i].startsWith("I")) {
                iContacts.add(temp[i]);
            } else if (temp[i].startsWith("J")) {
                jContacts.add(temp[i]);
            } else if (temp[i].startsWith("K")) {
                kContacts.add(temp[i]);
            } else if (temp[i].startsWith("L")) {
                lContacts.add(temp[i]);
            } else if (temp[i].startsWith("M")) {
                mContacts.add(temp[i]);
            } else if (temp[i].startsWith("N")) {
                nContacts.add(temp[i]);
            } else if (temp[i].startsWith("O")) {
                oContacts.add(temp[i]);
            } else if (temp[i].startsWith("P")) {
                pContacts.add(temp[i]);
            } else if (temp[i].startsWith("Q")) {
                qContacts.add(temp[i]);
            } else if (temp[i].startsWith("R")) {
                rContacts.add(temp[i]);
            } else if (temp[i].startsWith("S")) {
                sContacts.add(temp[i]);
            } else if (temp[i].startsWith("T")) {
                tContacts.add(temp[i]);
            } else if (temp[i].startsWith("U")) {
                uContacts.add(temp[i]);
            } else if (temp[i].startsWith("V")) {
                vContacts.add(temp[i]);
            } else if (temp[i].startsWith("W")) {
                wContacts.add(temp[i]);
            } else if (temp[i].startsWith("X")) {
                xContacts.add(temp[i]);
            } else if (temp[i].startsWith("Y")) {
                yContacts.add(temp[i]);
            } else if (temp[i].startsWith("Z")) {
                zContacts.add(temp[i]);
            }
        }

        int bSize = aContacts.size();
        int cSize = bSize + bContacts.size();
        int dSize = cSize + cContacts.size();
        int eSize = dSize + dContacts.size();
        int fSize = eSize + eContacts.size();
        int gSize = fSize + fContacts.size();//
        int hSize = gSize + gContacts.size();
        int iSize = hSize + hContacts.size();
        int jSize = iSize + iContacts.size();
        int kSize = jSize + jContacts.size();
        int lSize = kSize + kContacts.size();
        int mSize = lSize + lContacts.size();
        int nSize = mSize + mContacts.size();
        int oSize = nSize + nContacts.size();
        int pSize = oSize + oContacts.size();
        int qSize = pSize + pContacts.size();
        int rSize = qSize + qContacts.size();
        int sSize = rSize + rContacts.size();
        int tSize = sSize + sContacts.size();
        int uSize = tSize + tContacts.size();
        int vSize = uSize + uContacts.size();
        int wSize = vSize + vContacts.size();
        int xSize = wSize + wContacts.size();
        int ySize = xSize + xContacts.size();
        int zSize = ySize + yContacts.size();

        if (target < bSize) {
            bubbleTextString = "A";
        } else {
            if (target < cSize) {
                bubbleTextString = "B";
            } else {
                if (target < dSize) {
                    bubbleTextString = "C";
                } else {
                    if (target < eSize) {
                        bubbleTextString = "D";
                    } else {
                        if (target < fSize) {
                            bubbleTextString = "E";
                        } else {
                            if (target < gSize) {
                                bubbleTextString = "F";
                            } else {
                                if (target < hSize) {
                                    bubbleTextString = "G";
                                } else {
                                    if (target < iSize) {
                                        bubbleTextString = "H";
                                    } else if (target < jSize) {
                                        bubbleTextString = "I";
                                    } else {
                                        if (target < kSize) {
                                            bubbleTextString = "J";
                                        } else {
                                            if (target < lSize) {
                                                bubbleTextString = "K";
                                            } else {
                                                if (target < mSize) {
                                                    bubbleTextString = "L";
                                                } else {
                                                    if (target < nSize) {
                                                        bubbleTextString = "M";
                                                    } else {
                                                        if (target < oSize) {
                                                            bubbleTextString = "N";
                                                        } else {
                                                            if (target < pSize) {
                                                                bubbleTextString = "O";
                                                            } else {
                                                                if (target < qSize) {
                                                                    bubbleTextString = "P";
                                                                } else {
                                                                    if (target < rSize) {
                                                                        bubbleTextString = "Q";
                                                                    } else {
                                                                        if (target < sSize) {
                                                                            bubbleTextString = "R";
                                                                        } else {
                                                                            if (target < tSize) {
                                                                                bubbleTextString = "S";
                                                                            } else {
                                                                                if (target < uSize) {
                                                                                    bubbleTextString = "T";
                                                                                } else {
                                                                                    if (target < vSize) {
                                                                                        bubbleTextString = "U";
                                                                                    } else {
                                                                                        if (target < wSize) {
                                                                                            bubbleTextString = "V";
                                                                                        } else {
                                                                                            if (target < xSize) {
                                                                                                bubbleTextString = "W";
                                                                                            } else {
                                                                                                if (target < ySize) {
                                                                                                    bubbleTextString = "X";
                                                                                                } else {
                                                                                                    if (target < zSize) {
                                                                                                        bubbleTextString = "Y";
                                                                                                    } else {
                                                                                                        bubbleTextString = "Z";
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return bubbleTextString;
    }
}