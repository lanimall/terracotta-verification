package org.terracotta.utils;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.search.*;
import net.sf.ehcache.search.query.QueryManager;
import net.sf.ehcache.search.query.QueryManagerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.utils.domain.Customer;

import java.util.*;
import java.util.concurrent.*;

public class Launcher {
    public static final String ENV_CACHE_NAME = "ehcache.config.cachename";
    public static final long DEFAULT_MULTI_OPERATION_SLEEPINTERVAL = 5000;
    public static final String LOOP_MODIFIER = "{loop}";
    public static final String ARGS_SEPARATOR = " ";
    public static final String COMMAND_SEPARATOR = "~~";
    private static Logger log = LoggerFactory.getLogger(Launcher.class);
    private static int operationIdCounter = 1;
    private final Ehcache cache;
    private final QueryManager queryManager;
    private final int DEFAULT_NBOFELEMENTS = 10;
    private final String default_value = "Terracotta";
    private final CompletionService completionCacheBulkOpsService;

    private final int CACHE_OPS_POOLSIZE = 8;
    private Random rdm = new Random(System.nanoTime());

    public Launcher() throws Exception {
        ExecutorService cacheBulkOpsService = Executors.newFixedThreadPool(CACHE_OPS_POOLSIZE, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Cache Bulk Ops Service");
            }
        });

        //wrapping the executor in a CompletionService for easy access ot the finished tasks
        completionCacheBulkOpsService = new ExecutorCompletionService(cacheBulkOpsService);

        String cacheName = "";
        if (null != System.getProperty(ENV_CACHE_NAME)) {
            cacheName = System.getProperty(ENV_CACHE_NAME);
        }

        cache = EhcacheSingleton.instance.getCache(cacheName);
        if (cache == null) {
            log.info("Could not find the cache " + cacheName + ". Make sure sys property " + ENV_CACHE_NAME + " is set correctly. Exiting.");
            System.exit(0);
        }

        TerracottaClientConfiguration tcConfig = cache.getCacheManager().getConfiguration().getTerracottaConfiguration();
        if(null == tcConfig){
            log.info("CacheManager is *not* setup for Terracotta...");
        } else {
            log.info("CacheManager *is* setup for Terracotta...");
        }

        //instantiate the query manager for sql searches
        queryManager = QueryManagerBuilder
                .newQueryManagerBuilder()
                .addCache(cache)
                .build();
    }

    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher();
        launcher.run(args);
        log.info("Completed");
        System.exit(0);
    }

    public void run(String[] args) throws Exception {
        printLineSeparator();
        boolean keepRunning = true;
        while (keepRunning) {
            if (null != args && args.length > 0) {
                //there could be several command chained together with comma...hence let's try to find it out
                String joinedCommand = joinStringArray(args, ARGS_SEPARATOR);

                boolean doLoop = false;
                if (joinedCommand.startsWith(LOOP_MODIFIER)) {
                    doLoop = true;
                    joinedCommand = joinedCommand.substring(LOOP_MODIFIER.length());
                }

                if (log.isDebugEnabled())
                    log.debug("Full command: " + joinedCommand);

                String[] multipleCommands = joinedCommand.split(COMMAND_SEPARATOR);
                do {
                    for (String inputCommand : multipleCommands) {
                        processInput(inputCommand);
                        Thread.sleep(DEFAULT_MULTI_OPERATION_SLEEPINTERVAL);
                    }
                } while (doLoop);

                //if args are specified directly, it should run once and exit (useful for batch scripting)
                keepRunning = false;
            } else {
                printOptions();
                String input = getInput();
                if (input.length() == 0) {
                    continue;
                }

                if (log.isDebugEnabled())
                    log.debug("Full command: " + input);

                String[] multipleCommands = input.split(COMMAND_SEPARATOR);
                for (String inputCommand : multipleCommands) {
                    keepRunning = processInput(inputCommand);
                    if (!keepRunning)
                        break;
                }
            }
        }
    }

    private String joinStringArray(String[] arr, String separator) {
        String join = "";
        if (null != arr) {
            for (String s : arr) {
                if (join.length() > 0)
                    join += separator;
                join += s;
            }
        }
        return join;
    }

    private String getInput() throws Exception {
        System.out.println(">>");

        // option1
        Scanner sc = new Scanner(System.in);
        sc.useDelimiter(System.getProperty("line.separator"));
        return sc.nextLine();
    }

    public boolean processInput(String input) throws Exception {
        String[] inputs = input.split(" ");
        return processInput(inputs);
    }

    public boolean processInput(String[] args) throws Exception {
        String[] inputArgs = null;
        String inputCommand = "";
        if (null != args && args.length > 0) {
            inputCommand = args[0];
            if (args.length > 1) {
                inputArgs = Arrays.copyOfRange(args, 1, args.length);
            }
        }

        //parsing operation
        OPERATIONS command = OPERATIONS.getById(inputCommand);
        if(null == command)
            throw new IllegalArgumentException("Unrecognized command");

        boolean returnValue = false;
        try {
            returnValue = processInput(command, inputArgs);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnValue;
    }

    public void printLineSeparator() {
        String lineSeparator = System.getProperty("line.separator");
        byte[] bytes = lineSeparator.getBytes();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b) + " ");
        }
        log.info("Line separator = " + lineSeparator + " (hex = " + sb.toString() + ")");
    }

    public void printOptions() {
        System.out.println("");
        System.out.println("");
        System.out.println("What do you want to do now?");
        for (OPERATIONS op : OPERATIONS.values()) {
            System.out.println(op.toString());
        }
    }

    private boolean processInput(OPERATIONS command, String[] args) throws Exception {
        log.info("######################## Processing command... ############################");
        int cacheKey = -1;
        String cacheValue = null;
        boolean doNotDisplayEachElement = false;
        boolean doGetAllKeysExpiryCheck = false;
        boolean enableBulkMode = false;

        //get the operation based on input
        log.info(String.format("Command: \"%s\" with params \"%s\"", command.opDetail, joinStringArray(args, ARGS_SEPARATOR)));
        try {
            switch (command) {
                case OP_LOAD:
                    int nbOfElements = DEFAULT_NBOFELEMENTS;
                    if (null != args && args.length > 0) {
                        try {
                            nbOfElements = Integer.parseInt(args[0]);
                        } catch (NumberFormatException nfe) {
                            nbOfElements = DEFAULT_NBOFELEMENTS;
                        }

                        if (args.length > 1)
                            enableBulkMode = Boolean.parseBoolean(args[1]);
                    }

                    loadCache(nbOfElements, enableBulkMode);
                    break;
                case OP_CONTAINSKEY:
                    if (null != args && args.length > 0) {
                        cacheKey = Integer.parseInt(args[0]);
                        if (args.length > 1)
                            doGetAllKeysExpiryCheck = Boolean.parseBoolean(args[1]);
                    }

                    containsCacheElement(cacheKey, doGetAllKeysExpiryCheck);
                    break;
                case OP_SQL:
                    String sqlQuery = joinStringArray(args, ARGS_SEPARATOR);
                    searchInCache(sqlQuery.trim());
                    break;
                case OP_GETSINGLE:
                    if (null != args && args.length > 0) {
                        cacheKey = Integer.parseInt(args[0]);
                    }

                    displayCacheElement(cacheKey, true, true);
                    break;
                case OP_GETALL:
                    if (null != args && args.length > 0) {
                        doGetAllKeysExpiryCheck = Boolean.parseBoolean(args[0]);

                        if (args.length > 1)
                            doNotDisplayEachElement = Boolean.parseBoolean(args[1]);
                    }

                    displayAllCacheElements(doGetAllKeysExpiryCheck, !doNotDisplayEachElement);
                    break;
                case OP_ADDNEW:
                    if (null != args && args.length > 0) {
                        cacheValue = args[0];
                        for (int i = 1; i < args.length; i++) cacheValue += " " + args[i];
                    }

                    addNewElementToCache(cacheValue);
                    break;
                case OP_UPDATE:
                    if (null != args && args.length > 0) {
                        cacheKey = Integer.parseInt(args[0]);
                        if (args.length > 1) {
                            cacheValue = args[1];
                            for (int i = 2; i < args.length; i++) cacheValue += " " + args[i];
                        }
                    }

                    updateExistingCacheElement(cacheKey, cacheValue);
                    break;
                case OP_DELETESINGLE:
                    if (null != args && args.length > 0) {
                        cacheKey = Integer.parseInt(args[0]);
                    }

                    deleteCacheElement(cacheKey);
                    break;
                case OP_DELETEALL:
                    deleteAllCacheElements();
                    break;
                case OP_SIZE:
                    displayCacheSize();
                    break;
                case OP_QUIT:
                    return false;
                default:
                    log.info(String.format("Unrecognized command: %s %s", command.opInput, joinStringArray(args, ARGS_SEPARATOR)));
                    break;
            }
        } catch (Exception e) {
            log.info(String.format("Exception occurred: %s", e.getMessage()));
        }

        log.info("#########################################################################");
        return true;
    }

    private void loadCache(int nbOfElements, boolean enableBulk) throws InterruptedException, ExecutionException {
        synchronized (completionCacheBulkOpsService) {
            displayCacheSize();
            if (enableBulk)
                cache.setNodeBulkLoadEnabled(true);

            try {
                log.info(String.format("Adding %d entries to the cache", nbOfElements));
                int keyOffSet = cache.getSize();

                long start = System.nanoTime();

                int submitCount = 0;
                while (submitCount < nbOfElements) {
                    completionCacheBulkOpsService.submit(new PutOp(keyOffSet + submitCount, Customer.createRandom(keyOffSet + submitCount, null)), true);
                    submitCount++;
                }

                int doneCount = 0;
                while (doneCount < submitCount) {
                    completionCacheBulkOpsService.take();
                    doneCount++;
                }

                displayTiming("Operation loadCache()", start);
            } finally {
                if (enableBulk)
                    cache.setNodeBulkLoadEnabled(false);
            }

            displayCacheSize();
        }
    }

    @SuppressWarnings("unchecked")
    private void displayAllCacheElements(boolean doGetAllKeysExpiryCheck, boolean displayEachElement) throws InterruptedException, ExecutionException {
        synchronized (completionCacheBulkOpsService) {
            List<Object> keys = getAllKeys(doGetAllKeysExpiryCheck);

            long start = System.nanoTime();
            if (null != keys) {
                Iterator<?> it = keys.iterator();

                int submitCount = 0;
                while (it.hasNext()) {
                    completionCacheBulkOpsService.submit(new GetOp(it.next(), displayEachElement), true);
                    submitCount++;
                }

                //waiting for all futures to be fetched
                int doneCount = 0;
                while (doneCount < submitCount) {
                    completionCacheBulkOpsService.take();
                    doneCount++;
                }
            }

            displayTiming("Operation getAll()", start);
            displayCacheSize();
        }
    }

    private void displayTiming(String prefix, long startTimeNanos) {
        log.info(String.format("%s - Duration: %.3f ms", prefix, (System.nanoTime() - startTimeNanos) / 1000000F));
    }

    private void displayCacheSize() {
        log.info("Total no. of element in the cache = " + cache.getSize());
    }

    private void addNewElementToCache(String value) {
        displayCacheSize();

        int key = cache.getSize() + 1;
        Customer customer = Customer.createRandom(key, value);
        add(key, customer);

        log.info("Successfully added cache entry " + customer.toString() + " with key " + key);
        displayCacheSize();
    }

    private void updateExistingCacheElement(int keyToUpdate, String valueToUpdate) {
        displayCacheSize();

        Object valObject = get(keyToUpdate);
        if (null != valObject) {
            if (valObject instanceof Customer) {
                if (null != valueToUpdate && !"".equals(valueToUpdate))
                    ((Customer) valObject).setUserName(valueToUpdate);
                else
                    ((Customer) valObject).setUserName(Customer.DEFAULT_USERNAME + "-" + Integer.toString(cache.getSize() + 1));

                add(keyToUpdate, valObject);
                log.info("Successfully updated cache entry with key " + keyToUpdate + " to " + valObject.toString());
            } else {
                log.error("Unexpected cache object type");
            }
        } else {
            log.info("Could not find cache entry with key " + keyToUpdate);
        }

        displayCacheSize();
    }

    private void deleteCacheElement(int keyToDelete) {
        displayCacheSize();

        remove(keyToDelete);
        log.info("Successfully deleted user from cache with key=" + keyToDelete);

        displayCacheSize();
    }

    private void deleteAllCacheElements() {
        displayCacheSize();

        remove(null);
        log.info("Successfully deleted all entries from cache");

        displayCacheSize();
    }

    private void displayCacheElement(Object keyToGet, boolean displayElements, boolean displayTiming) {
        Object valueObj = get(keyToGet, displayTiming);
        if (displayElements) {
            if (null != valueObj) {
                log.info("Successfully retrieved key from cache: key=[" + keyToGet.toString() + "] - value=[" + valueObj.toString() + "]");
            } else {
                log.info("Key " + keyToGet + " is not in cache");
            }
        }
    }

    private void containsCacheElement(Object keyToSearch, boolean doGetAllKeysExpiryCheck) {
        if (containsCacheKey(keyToSearch, doGetAllKeysExpiryCheck)) {
            log.info("Key " + keyToSearch + " is in cache" + ((!doGetAllKeysExpiryCheck) ? " but could be expired" : " and non-expired"));
        } else {
            log.info("Key " + keyToSearch + " is not in cache");
        }
    }

    private boolean containsCacheKey(Object key, boolean doGetAllKeysExpiryCheck) {
        boolean isKeyInCache = false;
        if (doGetAllKeysExpiryCheck) {
            long start = System.nanoTime();
            Element element = cache.get(key);
            isKeyInCache = (null != element);

            displayTiming("Operation cache.get(key)", start);
        } else {
            long start = System.nanoTime();
            isKeyInCache = cache.isKeyInCache(key);
            displayTiming("Operation cache.isKeyInCache(key)", start);
        }

        return isKeyInCache;
    }

    private void add(Object key, Object value) {
        add(key, value, true);
    }

    private void add(Object key, Object value, boolean displayTiming) {
        long start = System.nanoTime();
        cache.put(new Element(key, value));

        if (displayTiming)
            displayTiming("Operation cache.put(new Element(key, value))", start);
    }

    private Object get(Object key) {
        return get(key, true);
    }

    private Object get(Object key, boolean displayTiming) {
        Object valueObj = null;

        long start = System.nanoTime();

        Element element = cache.get(key);
        if (null != element) {
            valueObj = element.getObjectValue();
        }

        if (displayTiming)
            displayTiming("Operation cache.get(key)", start);

        return valueObj;
    }

    @SuppressWarnings("rawtypes")
    private List getAllKeys(boolean doGetAllKeysExpiryCheck) {
        List keys = null;
        if (doGetAllKeysExpiryCheck) {
            long start = System.nanoTime();
            keys = cache.getKeysWithExpiryCheck();
            displayTiming("Operation cache.getKeysWithExpiryCheck()", start);
        } else {
            long start = System.nanoTime();
            keys = cache.getKeys();
            displayTiming("Operation cache.getKeys()", start);
        }

        return keys;
    }

    private void searchInCache(String sqlQuery) {
        boolean isGroupBy = sqlQuery.contains("group by");

        Query query = queryManager.createQuery(sqlQuery);
        if(!isGroupBy)
            query
                    .includeKeys()
                    .includeValues();

        long start = System.nanoTime();
        Results queryResult = query.end().execute();
        displayTiming("Query \"" + sqlQuery + "\"", start);

        if(isGroupBy || queryResult.hasAggregators()) {
            List aggResult = null;
            if(queryResult.hasAggregators()) {
                aggResult = queryResult.all().iterator().next().getAggregatorResults();
            } else {
                aggResult = queryResult.all();
            }
            for (Object o : aggResult) {
                log.info("Result: {}", o.toString());
            }
        } else {
            if (queryResult.size() > 0) {
                log.info("Retrieved " + queryResult.size() + " items from cache.");
                log.info("Details ----------------------------");
                for (Result rslt : queryResult.all()) {
                    Object key = rslt.getKey();
                    Object value = rslt.getValue();
                    log.info("Successfully retrieved key from cache: key=[" + key.toString() + "] - value=[" + value.toString() + "]");
                }
                log.info("End Details ----------------------------");
                log.info("Retrieved " + queryResult.size() + " items from cache.");
            } else {
                log.info("No result found.");
            }
        }
    }

    private void remove(Object key) {
        if (null == key) {
            long start = System.nanoTime();
            cache.removeAll();
            displayTiming("Operation cache.removeAll()", start);
        } else {
            long start = System.nanoTime();
            cache.remove(key);
            displayTiming("Operation cache.remove(key)", start);
        }

    }

    private enum OPERATIONS {
        OP_LOAD("Load cache with elements (@@opInput@@ <number of elements> <BulkMode=true|false*>)"),
        OP_GETSINGLE("Display a cache element (@@opInput@@ <key>)"),
        OP_GETALL("Display all cache elements (@@opInput@@ <ExpiryCheck=true|false*> <DoNotDisplayEach=true|false*>)"),
        OP_SQL("Query the cache with SQL (@@opInput@@ <sql>)"),
        OP_CONTAINSKEY("Check if key is in cache (@@opInput@@ <key> <ExpiryCheck=true|false*>)"),
        OP_ADDNEW("Add new element to cache (@@opInput@@ <username>)"),
        OP_UPDATE("Update an existing cache element (@@opInput@@ <key to update> <new username>)"),
        OP_DELETESINGLE("Delete a cache element by key (@@opInput@@ <key to delete>)"),
        OP_DELETEALL("Remove all cache entries"),
        OP_SIZE("Display cache size"),
        OP_QUIT("Quit program");

        private String opInput;
        private String opDetail;

        private OPERATIONS() {
            this(operationIdCounter++, "");
        }

        private OPERATIONS(String opDetail) {
            this(operationIdCounter++, opDetail);
        }

        private OPERATIONS(int opInput, String opDetail) {
            this(new Integer(opInput).toString(), opDetail);
        }

        private OPERATIONS(String opInput, String opDetail) {
            this.opInput = opInput;
            if (null != opDetail) {
                opDetail = opDetail.replaceAll("@@opInput@@", String.valueOf(opInput));
            }
            this.opDetail = opDetail;
        }

        public static OPERATIONS getById(String input) {
            for (OPERATIONS op : values()) {
                if (op.opInput.equalsIgnoreCase(input))
                    return op;
            }
            return null;
        }

        @Override
        public String toString() {
            return String.valueOf(opInput) + " - " + opDetail;
        }
    }

    /*
     * Performs get operation: first check in underlying cache, then if not found, in delegated cache.
     */
    private class GetOp implements Runnable {
        private final Object key;
        private final boolean displayElements;

        private GetOp(Object key, boolean displayElements) {
            super();
            this.key = key;
            this.displayElements = displayElements;
        }

        @Override
        public void run() {
            displayCacheElement(key, displayElements, false);
        }
    }

    /*
     * Performs get operation: first check in underlying cache, then if not found, in delegated cache.
     */
    private class PutOp implements Runnable {
        private Object key;
        private Object value;

        public PutOp(Object key, Object value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public void run() {
            cache.put(new Element(key, value));
        }
    }
}