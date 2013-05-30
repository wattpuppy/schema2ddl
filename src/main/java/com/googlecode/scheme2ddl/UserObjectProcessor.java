package com.googlecode.scheme2ddl;

import com.googlecode.scheme2ddl.dao.UserObjectDao;
import com.googlecode.scheme2ddl.domain.UserObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemProcessor;

import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Properties;

import static com.googlecode.scheme2ddl.TypeNamesUtil.map2TypeForDBMS;

/**
 * @author A_Reshetnikov
 * @since Date: 17.10.2012
 */
public class UserObjectProcessor implements ItemProcessor<UserObject, UserObject> {

    private static final Log log = LogFactory.getLog(UserObjectProcessor.class);
    private UserObjectDao userObjectDao;
    private DDLFormatter ddlFormatter;
    private FileNameConstructor fileNameConstructor;
    private Map<String, Set<String>>    excludes;
    private Map<String, Set<String>>    dependencies;
    private Map<String, Boolean>        settingsUserObjectProcessor;
    private Map<String, Set<String>>    dependenciesInSeparateFiles;
    private ArrayList<String>           excludesDataTables;
    private Map<String, Properties>     includesDataTables;
    private boolean isUsedSchemaNamesInFilters = false;
    private boolean isExportDataTable = false;

    public UserObject process(UserObject userObject) throws Exception {

        if (needToExclude(userObject)) {
            log.debug(String.format("Skipping processing of user object %s ", userObject));
            return null;
        }
        userObject.setDdl(map2Ddl(userObject));
        userObject.setFileName(fileNameConstructor.map2FileName(userObject));

        //if (userObject.getType.equals("TABLE")) {
            /*
             * if "isExportDataTable" { blablabla }
             *   patch for export data table will later
             */
        //}
        return userObject;
    }

    private boolean needToExclude(UserObject userObject) {
        if (excludes == null || excludes.size() == 0) return false;

        String fullTableName;
        if (isUsedSchemaNamesInFilters && userObject.getSchema() != null) {
            fullTableName = userObject.getSchema() + "." + userObject.getName();
        } else {
            fullTableName = userObject.getName();
        }

        if (excludes.get("*") != null) {
            for (String pattern : excludes.get("*")) {
                if (matchesByPattern(fullTableName, pattern))
                    return true;
            }
        }
        for (String typeName : excludes.keySet()) {
            if (typeName.equalsIgnoreCase(userObject.getType())) {
                if (excludes.get(typeName) == null) return true;
                for (String pattern : excludes.get(typeName)) {
                    if (matchesByPattern(fullTableName, pattern))
                        return true;
                }
            }
        }
        return false;
    }

    private boolean matchesByPattern(String s, String pattern) {
        pattern = pattern.replace("*", "(.*)").toLowerCase();
        return s.toLowerCase().matches(pattern);
    }

    private String map2Ddl(UserObject userObject) {
        if (userObject.getType().equals("DBMS JOB")) {
            return userObjectDao.findDbmsJobDDL(userObject.getName());
        }
        if (userObject.getType().equals("PUBLIC DATABASE LINK")) {
            return userObjectDao.findDDLInPublicScheme(map2TypeForDBMS(userObject.getType()), userObject.getName());
        }
        String res = userObjectDao.findPrimaryDDL(map2TypeForDBMS(userObject.getType()), userObject.getName());
        Set<String> dependedTypes = dependencies.get(userObject.getType());
        if (dependedTypes != null) {
            for (String dependedType : dependedTypes) {
                String resultDDL = userObjectDao.findDependentDLLByTypeName(dependedType, userObject.getName());

                if (dependenciesInSeparateFiles != null
                        && dependenciesInSeparateFiles.get(userObject.getType()) != null
                        && dependenciesInSeparateFiles.get(userObject.getType()).contains(dependedType))
                {
                    if (resultDDL != null && !resultDDL.equals("")) {
                        userObject.setDependentDDL(dependedType,  ddlFormatter.formatDDL(resultDDL),
                                fileNameConstructor.map2FileNameRaw(userObject.getSchema(), dependedType, userObject.getName()));
                    }
                } else {
                    if (ddlFormatter.getIsMorePrettyFormat())
                        res += ddlFormatter.newline;
                    res += resultDDL;
                }
            }
        }
        return ddlFormatter.formatDDL(res);
    }


    public void setExcludes(Map excludes) {
        this.excludes = excludes;
    }

    public void setExcludesDataTables(ArrayList excludesDataTables) {
        this.excludesDataTables = excludesDataTables;
    }

    public void setIncludesDataTables(Map includesDataTables) {
        this.includesDataTables = includesDataTables;
    }

    public void setDependencies(Map<String, Set<String>> dependencies) {
        this.dependencies = dependencies;
    }

    public void setDependenciesInSeparateFiles(Map<String, Set<String>> dependenciesInSeparateFiles) {
        this.dependenciesInSeparateFiles = dependenciesInSeparateFiles;
    }

    public void setUserObjectDao(UserObjectDao userObjectDao) {
        this.userObjectDao = userObjectDao;
    }

    public void setDdlFormatter(DDLFormatter ddlFormatter) {
        this.ddlFormatter = ddlFormatter;
    }

    public void setFileNameConstructor(FileNameConstructor fileNameConstructor) {
        this.fileNameConstructor = fileNameConstructor;
    }

    public void setSettingsUserObjectProcessor(Map<String, Boolean> settingsUserObjectProcessor) {
        this.settingsUserObjectProcessor = settingsUserObjectProcessor;

        if (settingsUserObjectProcessor.get("isUsedSchemaNamesInFilters")) {
            isUsedSchemaNamesInFilters = true;
        }
        if (settingsUserObjectProcessor.get("isExportDataTable")) {
            isExportDataTable = true;
        }
    }

}
