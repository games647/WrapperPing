# WrapperPing

### Description

The [Minecraft Remote Toolkit](https://bukkit.org/threads/remotetoolkit-restarts-crash-detection-auto-saves-remote-console.674/)
is a nice toolkit to control your server. Although the wrapper itself is platform independent, the plugin for 
sending commands from the Minecraft server back to the wrapper is not. This project ports this Bukkit plugin to the 
Sponge platform. 

### Commands

    /holdsrv [username]:<password> - Shutdown the server instance and hold it
    /restartsrv [username]:<password> - Restart the server using the wrapper
    /reschedulerestart [username]:<password> <time> - Set a new restart time
    /stopwrapper [username]:<password> - Stops the wrapper isntance
    /ping - Sends pong back

### Permissions

    wrapperping.command.holdsrv
    wrapperping.command.restartsrv
    wrapperping.command.reschedulerestart
    wrapperping.command.stopwrapper
    wrapperping.command.ping

### Config

    # The wrapper port of the running server instance
    wrapper-port=1234
