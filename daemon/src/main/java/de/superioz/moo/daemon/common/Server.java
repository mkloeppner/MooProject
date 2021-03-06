package de.superioz.moo.daemon.common;

import de.superioz.moo.api.utils.IOUtil;
import de.superioz.moo.daemon.Daemon;
import de.superioz.moo.daemon.DaemonInstance;
import de.superioz.moo.daemon.util.ThreadableValue;
import de.superioz.moo.network.common.PacketMessenger;
import de.superioz.moo.network.exception.MooOutputException;
import de.superioz.moo.network.packets.PacketServerAttempt;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

@Getter
public class Server extends ServerFolder {

    public static final int DEFAULT_PORT = 25565;
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final String DEFAULT_RAM = "512M";
    private static final String STOP_COMMAND = "stop\n";

    private Process process;
    private DaemonInstance parent;
    private Console console;
    private boolean autoSave;
    private int id;
    private UUID uuid;

    private int port = DEFAULT_PORT;
    private String host = DEFAULT_HOST;
    private String ram = DEFAULT_RAM;
    private boolean online = false;

    @Setter
    private Consumer<Server> serverResult;

    public Server(DaemonInstance parent, int id, UUID uuid, File folder, boolean autoSave) {
        super(folder, parent.getStartFileName());
        this.uuid = uuid;
        this.parent = parent;
        this.id = id;
        this.autoSave = autoSave;
    }

    /**
     * Get the executor service
     *
     * @return The executors object
     */
    public ExecutorService getExecutors() {
        return parent.getExecutors();
    }

    /**
     * Checks if the server is startable
     *
     * @return The result
     */
    public boolean isStartable() {
        return getStartFile().exists() && getFolder().exists();
    }

    /**
     * Starts the server
     *
     * @param host The host
     * @param port The port
     * @param ram  The ram
     */
    public void start(String host, int port, String ram) {
        getExecutors().execute(() -> {
            boolean r = Server.this.run(host, port, ram);

            if(!r) return;
            try {
                Server.this.process.waitFor();
            }
            catch(InterruptedException e) {
                e.printStackTrace();
            }

            // process is finished
            // server is offline
            online = false;
            getParent().getStartedServerByUuid().remove(getUuid());
            console.close();
            Daemon.getInstance().getLogs().info("Server " + getName() + " #" + getId() + " closed.");

            // if autoSave then copy the folder first
            if(isAutoSave()) {
                try {
                    IOUtil.deleteFile(new File(getParent().getPatternFolder(), getName()));
                }
                catch(IOException e) {
                    Daemon.getInstance().getLogs().debug("Couldn't delete server folder " + getName() + "!");
                }
                IOUtil.copyFiles(getFolder(), new File(getParent().getPatternFolder(), getName()));
            }

            // Delete everything  :(
            try {
                IOUtil.deleteFile(getFolder());
            }
            catch(IOException e) {
                Daemon.getInstance().getLogs().debug("Couldn't delete server folder " + getFolder().getName() + "!");
            }
        });
    }

    public void start(int port) {
        this.start(DEFAULT_HOST, port, DEFAULT_RAM);
    }

    /**
     * Runs the server task and returns if the server has been started
     * (If the server is already started, the result is false)
     *
     * @param host The host
     * @param port The port
     * @param ram  The ram
     * @return The result
     */
    private boolean run(String host, int port, String ram) {
        try {
            PacketMessenger.message(new PacketServerAttempt(PacketServerAttempt.Type.START, getUuid()));
        }
        catch(MooOutputException e) {
            e.printStackTrace();
            if(serverResult != null) serverResult.accept(null);
            return false;
        }

        if(online || !isStartable()) {
            if(serverResult != null) serverResult.accept(null);
            return false;
        }
        this.host = host;
        this.port = port;
        this.ram = ram;

        List<String> parameter = new ArrayList<>();
        parameter.add(getStartFile().getAbsolutePath());
        if(port != DEFAULT_PORT) parameter.addAll(Arrays.asList("-p", port + ""));
        if(!host.equals(DEFAULT_HOST)) parameter.addAll(Arrays.asList("-h", host));
        if(!ram.isEmpty()) parameter.add("-Xmx" + ram);

        ProcessBuilder builder = new ProcessBuilder(parameter);
        builder.directory(getFolder());

        try {
            this.process = builder.start();
        }
        catch(IOException e) {
            Daemon.getInstance().getLogs().debug("Error during server process " + getName() + "!", e);
            process.destroy();
            if(serverResult != null) serverResult.accept(null);
            return false;
        }

        ThreadableValue<Boolean> done = new ThreadableValue<>(false);
        this.console = new Console(this, this).start(s -> {
            //Daemon.getInstance().getLogs().info("INFO(" + getPort() + "): " + s);

            if(!done.get()) {
                if(!s.contains("Done") || !s.contains("For help, type \"help\" or")) {
                    return;
                }
                Daemon.getInstance().getLogs().info("Server @" + getHost() + ":" + getPort() + " started.");

                online = true;
                done.set(!done.get());

                //
                try {
                    if(serverResult == null) {
                        // Wut?
                    }
                    else {
                        serverResult.accept(this);
                    }
                }
                catch(MooOutputException e) {
                    if(serverResult != null) serverResult.accept(null);
                }
            }
        }, s -> {
            //Daemon.getInstance().getLogs().info("ERROR(" + getPort() + "): " + s);
        });

        return process != null && process.isAlive();
    }

    private boolean run(int port) {
        return run(DEFAULT_HOST, port, DEFAULT_RAM);
    }

    /**
     * Stops the server
     *
     * @return The result or false if the server is already offline
     */
    public boolean stop() {
        try {
            PacketMessenger.message(new PacketServerAttempt(PacketServerAttempt.Type.SHUTDOWN, getUuid()));
        }
        catch(MooOutputException e) {
            return false;
        }

        if(!online) {
            return false;
        }
        console.write(STOP_COMMAND);
        return true;
    }

}
