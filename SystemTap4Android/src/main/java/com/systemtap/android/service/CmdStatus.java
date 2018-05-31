package com.systemtap.android.service;

import java.util.LinkedList;

public class CmdStatus {
    int returnCode;
    LinkedList<String> stdOut;
    LinkedList<String> stdErr;

    public CmdStatus() {
        returnCode = -1;
        stdOut = new LinkedList<String>();
        stdErr = new LinkedList<String>();
    }
}
