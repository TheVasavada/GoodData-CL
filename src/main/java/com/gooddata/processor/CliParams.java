package com.gooddata.processor;

import com.gooddata.exception.InvalidArgumentException;
import com.gooddata.naming.N;
import com.gooddata.util.FileUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * GoodData CLI parameters wrapper
 *
 * @author zd <zd@gooddata.com>
 * @version 1.0
 */
public class CliParams extends HashMap<String,String> {

    private static Logger l = Logger.getLogger(CliParams.class);

    public static String[] CLI_PARAM_USERNAME = {"username","u"};
    public static String[] CLI_PARAM_PASSWORD = {"password","p"};

    public static String[] CLI_PARAM_HOST = {"host","h"};
    public static String[] CLI_PARAM_FTP_HOST = {"ftphost","f"};
    public static String[] CLI_PARAM_PROJECT = {"project","i"};
    public static String[] CLI_PARAM_BACKEND = {"backend","b"};
    public static String[] CLI_PARAM_DB_USERNAME = {"dbusername","d"};
    public static String[] CLI_PARAM_DB_PASSWORD = {"dbpassword","c"};
    public static String[] CLI_PARAM_PROTO = {"proto","t"};
    public static String[] CLI_PARAM_EXECUTE = {"execute","e"};
    public static String CLI_PARAM_SCRIPT = "script";


    public static Option[] mandatoryOptions = {
        new Option(CLI_PARAM_USERNAME[1], CLI_PARAM_USERNAME[0], true, "GoodData username"),
        new Option(CLI_PARAM_PASSWORD[1], CLI_PARAM_PASSWORD[0], true, "GoodData password"),
    };

    public static Option[] optionalOptions = {
        new Option(CLI_PARAM_HOST[1], CLI_PARAM_HOST[0], true, "GoodData host"),
        new Option(CLI_PARAM_FTP_HOST[1], CLI_PARAM_FTP_HOST[0], true, "GoodData FTP host"),
        new Option(CLI_PARAM_PROJECT[1], CLI_PARAM_PROJECT[0], true, "GoodData project identifier (a string like nszfbgkr75otujmc4smtl6rf5pnmz9yl)"),
        new Option(CLI_PARAM_BACKEND[1], CLI_PARAM_BACKEND[0], true, "Database backend DERBY or MYSQL"),
        new Option(CLI_PARAM_DB_USERNAME[1], CLI_PARAM_DB_USERNAME[0], true, "Database backend username (not required for the local Derby SQL)"),
        new Option(CLI_PARAM_DB_PASSWORD[1], CLI_PARAM_DB_PASSWORD[0], true, "Database backend password (not required for the local Derby SQL)"),
        new Option(CLI_PARAM_PROTO[1], CLI_PARAM_PROTO[0], true, "HTTP or HTTPS"),
        new Option(CLI_PARAM_EXECUTE[1], CLI_PARAM_EXECUTE[0], true, "Commands and params to execute before the commands in provided files")
    };


    protected CliParams(CommandLine ln) throws InvalidArgumentException {
        parse(ln);
    }

    public static CliParams create(CommandLine l) throws InvalidArgumentException {
        return new CliParams(l);
    }

    public static Options getOptions() {
        Options ops = new Options();
        for( Option o : mandatoryOptions)
            ops.addOption(o);
        for( Option o : optionalOptions)
            ops.addOption(o);
        return ops;
    }

    protected void parse(CommandLine ln) throws InvalidArgumentException {
        for( Option o : mandatoryOptions) {
            String name = o.getLongOpt();
            if (ln.hasOption(name))
                this.put(name,ln.getOptionValue(name));
            else
                throw new InvalidArgumentException("Missing the '"+name+"' commandline parameter.");

        }

        for( Option o : optionalOptions) {
            String name = o.getLongOpt();
            if (ln.hasOption(name))
                this.put(name,ln.getOptionValue(name));
        }

        // use default host if there is no host in the CLI params
        if(!this.containsKey(CLI_PARAM_HOST[0])) {
            this.put(CLI_PARAM_HOST[0],Defaults.DEFAULT_HOST);
        }

        // create default FTP host if there is no host in the CLI params
        if(!this.containsKey(CLI_PARAM_FTP_HOST[0])) {
            String[] hcs = this.get(CLI_PARAM_HOST[0]).toString().split("\\.");
            if(hcs != null && hcs.length > 0) {
                String ftpHost = "";
                for(int i=0; i<hcs.length; i++) {
                    if(i>0)
                        ftpHost += "." + hcs[i];
                    else
                        ftpHost = hcs[i] + N.FTP_SRV_SUFFIX;
                }
                this.put(CLI_PARAM_FTP_HOST[0],ftpHost);
            }
            else
                throw new IllegalArgumentException("Invalid format of the GoodData REST API host: " +
                        this.get(CLI_PARAM_HOST[0]));

        }

        // use default protocol if there is no host in the CLI params
        if(!this.containsKey(CLI_PARAM_PROTO[0])) {
            this.put(CLI_PARAM_PROTO[0], Defaults.DEFAULT_PROTO);
        }
        else {
            String proto = ln.getOptionValue(CLI_PARAM_PROTO[0]).toLowerCase();
            if(!"http".equalsIgnoreCase(proto) && !"https".equalsIgnoreCase(proto))
                throw new InvalidArgumentException("Invalid '"+CLI_PARAM_PROTO[0]+"' parameter. Use HTTP or HTTPS.");
            this.put(CLI_PARAM_PROTO[0], proto);
        }

        // use default backend if there is no host in the CLI params
        if(!this.containsKey(CLI_PARAM_BACKEND[0])) {
            this.put(CLI_PARAM_BACKEND[0], Defaults.DEFAULT_BACKEND);
        }
        else {
            String b = ln.getOptionValue(CLI_PARAM_BACKEND[0]).toLowerCase();
            if(!"mysql".equalsIgnoreCase(b) && !"derby".equalsIgnoreCase(b))
                b = "derby";
            this.put(CLI_PARAM_BACKEND[0], b);
        }

        if (ln.getArgs().length == 0 && !ln.hasOption("execute")) {
            throw new InvalidArgumentException("No command has been given, quitting.");
        }
        String scripts = "";
        for (final String arg : ln.getArgs()) {
            if(scripts.length()>0)
                scripts += ","+arg;
            else
                scripts += arg;
        }
        this.put(CLI_PARAM_SCRIPT, scripts);
    }

    /**
     * Returns the help for commands
     * @return help text
     */
    public static String commandsHelp() {
        try {
        	final InputStream is = GdcDI.class.getResourceAsStream("/com/gooddata/processor/COMMANDS.txt");
        	if (is == null)
        		throw new IOException();
            return FileUtil.readStringFromStream(is);
        } catch (IOException e) {
            l.error("Could not read com/gooddata/processor/COMMANDS.txt");
        }
        return "";
    }

}
