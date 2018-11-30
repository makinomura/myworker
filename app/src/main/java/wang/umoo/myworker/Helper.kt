package wang.umoo.myworker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.os.Environment
import android.util.Log
import wang.umoo.myworker.util.ShellUtils
import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.function.IntPredicate

object Helper {

//    val FOREST_KEY_MAP_PATH = Environment.getExternalStorageDirectory().absolutePath + "/myworker/keys/forest_key.png"
    //    public final static String ENERGY_KEY_MAP_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/myworker/keys/energy_key.png";
//    val FRIEND_KEY_MAP_PATH = Environment.getExternalStorageDirectory().absolutePath + "/myworker/keys/friend_key.png"

    val ALIPAY_PACKAGE_NAME = "com.eg.android.AlipayGphone"
    val ALIPAY_HOME_NAME = "com.eg.android.AlipayGphone.AlipayLogin"
    val ALIPAY_H5_NAME = "com.alipay.mobile.nebulacore.ui.H5Activity"


    fun recognizeKeyPoints(conf: RecognizeConf): List<Point> {
        val result = ArrayList<Point>()

        val imageBM = BitmapFactory.decodeFile(Helper.screenShot)

        val workerCount = Runtime.getRuntime().availableProcessors() * 2 + 1
        val es = Executors.newFixedThreadPool(workerCount)
        val cdl = CountDownLatch(((conf.keyPointArea.second.x - conf.keyPointArea.first.x) / conf.keyPointStep.x + 1) * ((conf.keyPointArea.second.y - conf.keyPointArea.first.y) / conf.keyPointStep.y + 1))

        for (x in conf.keyPointArea.first.x..conf.keyPointArea.second.x step conf.keyPointStep.x) {
            for (y in conf.keyPointArea.first.y..conf.keyPointArea.second.y step conf.keyPointStep.y) {
                es.submit(RecognizeWorker(Point(x, y), imageBM, result, cdl, conf))
            }
        }

        try {
            cdl.await()
            es.shutdown()
            removeNullPoint(result)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        removeRepeatPoint(result, conf.repeatDistance)

        if (!result.isEmpty()) {
            val pointString = StringBuilder()
            for (point in result) {
                pointString.append(point.toString()).append(" ")
            }

            Log.d("point", pointString.toString())
        }

        return result
    }

    val energyPoints: List<Point>
        get() = recognizeKeyPoints(RecognizeConf(
                keyPointArea = Pair(Point(40, 330), Point(900, 1100)),
                keyPointStep = Point(20, 20),
                featurePointArea = Pair(Point(-30, -30), Point(30, 30)),
                featurePointStep = Point(10, 10),
                redPredicate = IntPredicate { r -> r in 0xA0..0xCF },
                greenPredicate = IntPredicate { g -> g in 0xD0..0xFF },
                bluePredicate = IntPredicate { b -> (b in 0x80..0x9F || b in 0x10..0x3F) },
                trustRate = 0.6,
                repeatDistance = 100.0
        ))

    val haveEnergyFriendPoints: List<Point>
        get() = recognizeKeyPoints(RecognizeConf(
                keyPointArea = Pair(Point(940, 200), Point(1039, 2000)),
                keyPointStep = Point(50, 50),
                featurePointArea = Pair(Point(0, 0), Point(50, 50)),
                featurePointStep = Point(2, 2),
                redPredicate = IntPredicate { r -> r in 0x10..0x3F },
                greenPredicate = IntPredicate { g -> g in 0x90..0xAF },
                bluePredicate = IntPredicate { b -> b in 0x10..0x9F },
                trustRate = 0.2,
                repeatDistance = 100.0
        ))

    val screenShot: String?
        get() {
            val dir = Environment.getExternalStorageDirectory().absolutePath + "/myworker/"

            val dirPath = File(dir)
            if (!dirPath.exists()) {
                dirPath.mkdirs()
            }

            val fileName = dir + "myi_" + System.currentTimeMillis() + ".png"

            if (ShellUtils.checkRootPermission()) {
                ShellUtils.execCommand("screencap $fileName", true)
                return fileName
            } else {
                return null
            }

        }

    val isAlipayTop: Boolean
        get() = isTop(ALIPAY_PACKAGE_NAME)

    val isAlipayHomeTop: Boolean
        get() = isTop(ALIPAY_HOME_NAME)

    val isAlipayH5Top: Boolean
        get() = isTop(ALIPAY_H5_NAME)

    fun compareBM(imagePath: String?, keyPath: String): List<Point> {

        val result = ArrayList<Point>()

        if (imagePath == null) {
            return result
        }

        val imageFile = File(imagePath)
        if (!imageFile.exists()) {
            return result
        }

        val options = BitmapFactory.Options()

        options.inPremultiplied = false
        val keyBM = BitmapFactory.decodeFile(keyPath, options)

        val inSampleSize = 8
        options.inSampleSize = inSampleSize
        val imageBM = BitmapFactory.decodeFile(imagePath, options)

        val workerCount = Runtime.getRuntime().availableProcessors() * 2 + 1
        val es = Executors.newFixedThreadPool(workerCount)
        val cdl = CountDownLatch((imageBM.height - keyBM.height) * (imageBM.width - keyBM.width))

        for (height in 0 until imageBM.height - keyBM.height) {
            for (width in 0 until imageBM.width - keyBM.width) {
                es.submit(CompareWorker(width, height, imageBM, keyBM, result, cdl))
            }
        }

        try {
            cdl.await()
            es.shutdown()
            removeNullPoint(result)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        for (point in result) {
            point.x = point.x * inSampleSize
            point.y = point.y * inSampleSize
        }

        deleteFile(imagePath)

        removeRepeatPoint(result, 50.0)

        if (!result.isEmpty()) {
            val pointString = StringBuilder()
            for (point in result) {
                pointString.append(point.toString()).append(" ")
            }

            Log.d("point", pointString.toString())
        }

        return result
    }

    fun removeNullPoint(points: MutableList<Point>) {

        val iterator = points.iterator()

        while (iterator.hasNext()) {
            val next = iterator.next()

            if (next == null) {
                iterator.remove()
            }
        }
    }

    fun removeRepeatPoint(points: MutableList<Point>, distence: Double) {
        val repeatList = ArrayList<Point>()

        for (p in points) {
            for (q in points) {
                if (p === q || repeatList.contains(q)) continue

                val length = Math.sqrt(Math.pow((p.x - q.x).toDouble(), 2.0) + Math.pow((p.y - q.y).toDouble(), 2.0))
                if (length <= distence) {
                    repeatList.add(p)
                    break
                }
            }
        }

        points.removeAll(repeatList)
    }

    fun openForestHome() {
        if (ShellUtils.checkRootPermission()) {
            ShellUtils.execCommand("am start -a android.intent.action.VIEW -d alipays://platformapi/startapp?appId=60000002", true)
        }
    }

    fun tap(point: Point) {
        if (ShellUtils.checkRootPermission()) {
            ShellUtils.execCommand("input tap " + point.x + " " + point.y, true)
        }
    }

    fun swapUp() {
        swap(Point(100, 1000), Point(100, 440))
    }

    fun killAlipay() {
        if (ShellUtils.checkRootPermission()) {
            ShellUtils.execCommand("ps -ef | grep $ALIPAY_PACKAGE_NAME | grep -v grep | awk '{print $2}' | xargs kill -9", true)
        }
    }

    fun deleteFile(filePath: String) {
        ShellUtils.execCommand("rm $filePath", false)
    }

    fun back() {

        if (ShellUtils.checkRootPermission()) {
            ShellUtils.execCommand("input keyevent 4", true)
        }
    }

    fun swap(l: Point, r: Point) {
        if (ShellUtils.checkRootPermission()) {
            ShellUtils.execCommand("input swipe " + l.x + " " + l.y + " " + r.x + " " + r.y + " 100", true)
        }
    }

    fun isTop(key: String): Boolean {
        if (ShellUtils.checkRootPermission()) {
            val result = ShellUtils.execCommand("dumpsys window | grep mCurrentFocus", true)

            Log.d("top", result.successMsg)
            return result.successMsg != null && result.successMsg.contains(key)
        }

        return false
    }

    internal class RecognizeWorker(private val keyPoint: Point, private val imageBM: Bitmap, private val result: MutableList<Point>, private val cdl: CountDownLatch,
                                   private val conf: RecognizeConf) : Runnable {

        override fun run() {
            try {

                var total = 0
                var match = 0

                for (dx in conf.featurePointArea.first.x..conf.featurePointArea.second.x step conf.featurePointStep.x) {
                    for (dy in conf.featurePointArea.first.y..conf.featurePointArea.second.y step conf.featurePointStep.y) {
                        val p = imageBM.getPixel(keyPoint.x + dx, keyPoint.y + dy)

                        if (conf.redPredicate.test(Color.red(p)) && conf.greenPredicate.test(Color.green(p)) && conf.bluePredicate.test(Color.blue(p))) {
                            match++
                        }

                        total++
                    }
                }

                if (match * 1f / total >= conf.trustRate) {
                    result.add(keyPoint)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cdl.countDown()
            }
        }
    }

    internal class CompareWorker(private val x: Int, private val y: Int, private val imageBM: Bitmap, private val keyBM: Bitmap, private val result: MutableList<Point>, private val cdl: CountDownLatch) : Runnable {

        override fun run() {
            try {
                var noMatch = 0
                outer@ for (dx in 0 until keyBM.height) {
                    for (dy in 0 until keyBM.width) {

                        if (imageBM.width <= x + dy || imageBM.height <= y + dx)
                            continue

                        val p = imageBM.getPixel(x + dy, y + dx)
                        val q = keyBM.getPixel(dy, dx)

                        if (p != q && !(Math.abs(Color.red(p) - Color.red(q)) < 0x1F
                                        && Math.abs(Color.green(p) - Color.green(q)) < 0x1F
                                        && Math.abs(Color.blue(p) - Color.blue(q)) < 0x1F)) {

                            noMatch++

                            if (noMatch * 1f / keyBM.height.toFloat() / keyBM.width.toFloat() > 0.1) {
                                break@outer
                            }
                        }
                    }
                }

                if (noMatch * 1f / keyBM.height.toFloat() / keyBM.width.toFloat() <= 0.1) {
                    result.add(Point(x, y))
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cdl.countDown()
            }
        }
    }

    data class RecognizeConf(var keyPointArea: Pair<Point, Point>,
                             var keyPointStep: Point,
                             var featurePointArea: Pair<Point, Point>,
                             var featurePointStep: Point,
                             var redPredicate: IntPredicate,
                             var greenPredicate: IntPredicate,
                             var bluePredicate: IntPredicate,
                             var trustRate: Double,
                             var repeatDistance: Double
    )
}
