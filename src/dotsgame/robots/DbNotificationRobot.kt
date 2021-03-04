package dotsgame.robots

import zDb.connectDb
import zUtils.ValWithLock
import zUtils.log.logHint
import zUtils.log.logWarning

typealias DbNotificationListenerCallback = (name: String, payload: String?) -> Unit
class DbNotificationRobot: AbstractRobot() {
    private var stopped: Boolean = false
    private val thread: Thread = Thread(this::threadProc, "DbNotificationRobot")
    private val listeners = ValWithLock(mutableListOf<Listener>())
    data class Listener(val name: String, val callback: DbNotificationListenerCallback) {
        var hasBeenSet = false
    }

    fun addListener(listener: Listener) {
        return listeners.locked {
            it.add(listener)
            thread.interrupt()
        }
    }

    fun removeListener(listener: Listener) {
        listeners.locked {
            it.remove(listener)
        }
    }

    fun dispatchNotification(name: String, payload: String?) {
        logHint("DbNotification {0}: {1}", name, payload)
        listeners.locked {
            for (listener in it) {
                try {
                    if (name == listener.name) {
                        listener.callback(name, payload)
                    }
                } catch (t: Throwable) {
                    logWarning("Callback {0} failed: {1}", listener, t)
                }
            }
        }
    }

    override fun start() {
        thread.start()
    }

    override fun stop(join: Boolean) {
        stopped = true
        thread.interrupt()
        if (join) {
            thread.join()
        }
    }

    private fun threadProc() {
        while (!stopped) {
            try {
                connectDb().use { db ->
                    while (!stopped) {
                        try {
                            listeners.locked {
                                for (listener in it) {
                                    if (!listener.hasBeenSet) {
                                        listener.hasBeenSet = true
                                        try {
                                            db.listenNotifications(listener.name)
                                        } catch (t: Throwable) {
                                            logWarning("Ошибка установки прослушки {0}: {1}", listener.name, t)
                                        }
                                    }
                                }
                            }
                            val notifications = db.waitNotifications(5000)
                            for (notification in notifications) {
                                dispatchNotification(notification.name, notification.payload)
                            }
                        } catch (t: InterruptedException) {
                            // Nothing
                        }
                    }
                }
            } catch (t: InterruptedException) {
                // Nothing
            } catch (t: Throwable) {
                logWarning("Ошибка: {0}", t)
                try {
                    Thread.sleep(1000)
                } catch (t: InterruptedException) {
                    break
                }
            }
        }
    }
}