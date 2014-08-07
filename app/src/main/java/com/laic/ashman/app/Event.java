package com.laic.ashman.app;

/**
 * Created by duduba on 14-8-4.
 */
public class Event {

    enum ET {

        TASK(1), UPLOAD(2);

        private int value;
        private ET(int value) {
            this.value = value;
        }
    }

}
