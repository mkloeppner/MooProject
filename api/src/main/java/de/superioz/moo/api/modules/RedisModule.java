package de.superioz.moo.api.modules;

import de.superioz.moo.api.cache.MooRedis;
import de.superioz.moo.api.module.Module;
import lombok.Getter;
import org.redisson.config.Config;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * This module is for connecting to Redis
 *
 * @see org.redisson.Redisson
 * @see de.superioz.moo.api.module.ModuleRegistry#register(Module...)
 */
@Getter
public class RedisModule extends Module {

    private File configFile;
    private Config config;
    private Logger logger;

    public RedisModule(File configFile, Logger logger) {
        this.logger = logger;
        this.configFile = configFile;

        try {
            this.config = configFile.getName().endsWith(".json") ? Config.fromJSON(configFile) : Config.fromYAML(configFile);
        }
        catch(IOException e) {
            logger.severe("Error while loading Redis config! " + e);
            super.finished(false);
        }
    }

    @Override
    public String getName() {
        return "redis";
    }

    @Override
    protected void onEnable() {
        if(config == null){
            logger.info("Can't connect to Redis because the config is null!");
            super.finished(false);
            return;
        }
        MooRedis.getInstance().connectRedis(config);
        logger.info("Redis connection status: " + (MooRedis.getInstance().isRedisConnected() ? "ON" : "off"));
    }

    @Override
    protected void onDisable() {
        MooRedis.getInstance().getClient().shutdown();
    }
}
