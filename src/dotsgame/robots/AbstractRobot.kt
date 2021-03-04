package dotsgame.robots

abstract class AbstractRobot {
    /**
     * Запуск робота
     */
    abstract fun start()

    /**
     * Остановка работа. Может вызываться несколько раз.
     * Если параметр [join] = true, метод ждет завершения работы робота
     */
    abstract fun stop(join: Boolean)

    companion object {
        val notificationRobot = DbNotificationRobot()
        private val scheduler = RobotScheduler()
        private val robotList = listOf(
            VerificationRobot()
        )

        fun startRobots() {
            notificationRobot.start()
            for (robot in robotList) {
                robot.start()
            }
        }

        fun stopRobots() {
            scheduler.close()
            notificationRobot.stop(true);
            for (robot in robotList) {
                robot.stop(false)
            }
            for (robot in robotList) {
                robot.stop(true)
            }
        }

    }
}