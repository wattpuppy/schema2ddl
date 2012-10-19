package com.googlecode.scheme2ddl;

import com.googlecode.scheme2ddl.dao.UserObjectDao;
import com.googlecode.scheme2ddl.domain.UserObject;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author A_Reshetnikov
 * @since Date: 17.10.2012
 */
public class UserObjectReader implements ItemReader<UserObject> {

    private static List<UserObject> list;
    private UserObjectDao userObjectDao;
    private boolean processPublicDbLinks = false;    //todo use
    private boolean processDbmsJobs = false;         //todo use


    public UserObject read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (list == null) {
            fillList();
        }
        if (list.size() == 0)
            return null;
        else
            return list.remove(0);
    }

    private void fillList() {
        list = userObjectDao.findListForProccessing(); //todo make it threadsafe

    }

    public void setUserObjectDao(UserObjectDao userObjectDao) {
        this.userObjectDao = userObjectDao;
    }
}
