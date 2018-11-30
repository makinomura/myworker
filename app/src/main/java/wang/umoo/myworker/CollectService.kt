package wang.umoo.myworker

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast

class CollectService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    private val lock = Any()
    private var worker: Thread? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onDestroy() {
        super.onDestroy()

        Helper.deleteFile(Environment.getExternalStorageDirectory().absolutePath + "/myworker/myi_*.png")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        if (intent.getBooleanExtra("begin", false)) {
            synchronized(lock) {
                if (worker != null && worker!!.isAlive && !worker!!.isInterrupted) {
                    toast("服务已在运行中")
                } else {
                    worker = WorkThread()
                    worker!!.start()
                }
            }
        } else if (intent.getBooleanExtra("end", false)) {
            synchronized(lock) {
                if (worker != null) {
                    worker!!.interrupt()
                }
            }
            toast("服务已停止")
        }

        return Service.START_STICKY

    }

    private fun toast(s: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel("myworker", "aaa", NotificationManager.IMPORTANCE_HIGH)
        nm.createNotificationChannel(channel)
        val n = Notification.Builder(this, "myworker")

        n.setContentTitle("日志")
                .setContentText(s)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_foreground))
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setBadgeIconType(R.mipmap.ic_launcher_round)
                .setWhen(System.currentTimeMillis())

        nm.notify(s.hashCode(), n.build())


        handler.post { Toast.makeText(applicationContext, s, Toast.LENGTH_SHORT).show() }
    }

    private inner class WorkThread : Thread() {

        @Throws(InterruptedException::class)
        private fun checkState() {
            if (isInterrupted) {
                throw InterruptedException("用户取消")
            } else if (!Helper.isAlipayTop) {
                throw InterruptedException("支付宝离开前台")
            }
        }

        @SuppressLint("DefaultLocale", "WrongConstant")
        override fun run() {
            val conf = Conf.read(getSharedPreferences("conf", Context.MODE_APPEND))

            try {
                toast("开始自动收取能量(请不要操作手机，按音量键停止)")

                Helper.killAlipay()
                Thread.sleep(500)
                toast("进入支付宝")

                Helper.openForestHome()

                var wait = 0

                while (!Helper.isAlipayH5Top) {
                    Thread.sleep(200)
                    wait += 200

                    if (wait >= 5000) {
                        throw InterruptedException("无法进入蚂蚁森林")
                    }
                }

                checkState()
                Thread.sleep(conf.forestHomeWait)
                val selfEnergyPoints = Helper.energyPoints
                if (!selfEnergyPoints.isEmpty()) {
                    for (sep in selfEnergyPoints) {
                        checkState()
                        toast("收取自己的能量 " + sep.toString())

                        Helper.tap(sep)
                        Thread.sleep(10)
                    }
                } else {
                    toast("自己没有能量可以收取")
                }

                checkState()
                Helper.swapUp()
                Thread.sleep(conf.friendRankWait)
                val friendsListPoints = Helper.haveEnergyFriendPoints

                if (!friendsListPoints.isEmpty()) {
                    toast("有" + friendsListPoints.size + "位好友可以收取能量")

                    for (i in friendsListPoints.indices) {
                        checkState()

                        toast("收取第" + (i + 1) + "位好友的能量 " + friendsListPoints[i].toString())
                        Helper.tap(friendsListPoints[i])
                        Thread.sleep(conf.friendHomeWait)

                        val friendEnergyPoints = Helper.energyPoints
                        if (!friendEnergyPoints.isEmpty()) {

                            for (fep in friendEnergyPoints) {
                                checkState()
                                toast("收取好友的能量 " + fep.toString())

                                Helper.tap(fep)
                                Thread.sleep(10)
                            }
                        } else {
                            toast("没有能量可以收取")
                        }
                        checkState()

                        Helper.back()
                        Thread.sleep(100)
                    }
                } else {
                    toast("没有好友可以收取能量")
                }

                toast("自动收能量结束")


            } catch (e: InterruptedException) {
                toast("自动收能量终止: " + e.message)
            }

        }

    }

    companion object {

        fun begin(context: Context) {
            val service = Intent(context, CollectService::class.java)
            service.putExtra("begin", true)
            context.startService(service)
        }

        fun end(context: Context) {

            val service = Intent(context, CollectService::class.java)
            service.putExtra("end", true)
            context.startService(service)
        }
    }
}
