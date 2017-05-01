package com.example.cissy.esm;

import android.view.KeyEvent;
import android.widget.LinearLayout;

/**
 * Created by Cissy on 4/22/2017.
 */

public class MyLayout extends LinearLayout {

    private DetectAppsService myWindow;

    public MyLayout(DetectAppsService myWindow)
    {
        super(myWindow);

        this.myWindow = myWindow;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
        {
            if (event.getAction() == KeyEvent.ACTION_DOWN  &&  event.getRepeatCount() == 0)
            {
                getKeyDispatcherState().startTracking(event, this);
                return true;

            }

            else if (event.getAction() == KeyEvent.ACTION_UP)
            {
                getKeyDispatcherState().handleUpEvent(event);

                if (event.isTracking() && !event.isCanceled())
                {
                    // dismiss your window:
                    myWindow.hide(true);

                    return true;
                }
            }
        }

        return super.dispatchKeyEvent(event);
    }
}
