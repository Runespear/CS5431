package org.cs5431.controller;

import org.cs5431.model.AccountLog;
import org.cs5431.model.User;

public class AccountsLogController {
    private User user;
    private AccountLog log; //should this be private?

    AccountsLogController(User user) {
        this.user = user;

        //TODO find associated log here
        //TODO set log to associated log
    }


    public void addLogEntry() {
        //TODO
    }

    public void logContents() {
        //TODO
    }
}
