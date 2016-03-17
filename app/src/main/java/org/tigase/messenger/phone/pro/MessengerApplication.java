package org.tigase.messenger.phone.pro;

import android.app.Application;

import org.tigase.messenger.phone.pro.utils.AvatarHelper;

/**
 * Created by bmalkow on 16.03.16.
 */
public class MessengerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AvatarHelper.initilize(this);
    }
}
