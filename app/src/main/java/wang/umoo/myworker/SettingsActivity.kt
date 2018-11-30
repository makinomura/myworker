package wang.umoo.myworker

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.CountDownTimer
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.EditText
import android.widget.Toast
import wang.umoo.myworker.util.ShellUtils
import wang.umoo.myworker.util.Wrapper
import java.lang.String.format

class SettingsActivity : AppCompatActivity() {

    lateinit var alipayHome: EditText
    lateinit var forestHome: EditText
    lateinit var friendRank: EditText
    lateinit var friendHome: EditText

    lateinit var conf: Conf

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        alipayHome = findViewById(R.id.alipayHome)
        forestHome = findViewById(R.id.forestHome)
        friendRank = findViewById(R.id.friendRank)
        friendHome = findViewById(R.id.friendHome)

        conf = Conf.read(getSharedPreferences("conf", Context.MODE_APPEND))
        alipayHome.setText(conf.alipayHomeWait.toString())
        forestHome.setText(conf.forestHomeWait.toString())
        friendRank.setText(conf.friendRankWait.toString())
        friendHome.setText(conf.friendHomeWait.toString())

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val fab = findViewById<View>(R.id.fab) as FloatingActionButton
        fab.setOnClickListener { view ->
            conf.save()
            Snackbar.make(view, "保存成功！", Snackbar.LENGTH_LONG)
                    .setAction("立即执行") {
                        CollectService.begin(this@SettingsActivity)
                    }.show()
        }


        if (!conf.rootChecked || !ShellUtils.checkRootPermission()) {
            val builder = AlertDialog.Builder(this)

            builder.setTitle("程序需要ROOT权限才能正常运行")
            val cl = DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> if (ShellUtils.checkRootPermission()) {
                        conf.rootChecked = true
                        gotoAlipay()
                    } else {
                        Toast.makeText(this@SettingsActivity, "ROOT授权失败！即将退出...", Toast.LENGTH_SHORT).show()
                        this@SettingsActivity.finishAndRemoveTask()
                    }
                    else -> this@SettingsActivity.finishAndRemoveTask()
                }
            }
            builder.setPositiveButton("去授权", cl)
            builder.setNegativeButton("退出", cl)
            builder.setOnDismissListener { this@SettingsActivity.finishAndRemoveTask() }

            val dialog = builder.create()
            dialog.show()
        } else {
            gotoAlipay()
        }
    }

    private fun gotoAlipay() {
        val timerWrapper = Wrapper.empty<CountDownTimer>()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("即将打开支付宝...")

        val listener = DialogInterface.OnClickListener { dialog, which ->
            when (which) {
                AlertDialog.BUTTON_POSITIVE -> {
                    CollectService.begin(this@SettingsActivity)
                    dialog.dismiss()
                }
                else ->

                    //                        SettingsActivity.this.finishAndRemoveTask();

                    timerWrapper.unwrap { t: CountDownTimer? -> t?.cancel() }

            }
        }
        builder.setPositiveButton("开始", listener)

        builder.setNegativeButton("取消", listener)
        builder.setOnDismissListener {
            timerWrapper.unwrap { t: CountDownTimer? -> t?.cancel() }
        }

        val alertDialog = builder.create()

        alertDialog.show()

        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val timer = object : CountDownTimer(2000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                positiveButton.setText(format("确定(%d)", millisUntilFinished / 1000))
            }

            override fun onFinish() {
                listener.onClick(alertDialog, AlertDialog.BUTTON_POSITIVE)
            }
        }

        timerWrapper.wrap(timer)
        timer.start()
    }

}
