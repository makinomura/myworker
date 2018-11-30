package wang.umoo.myworker

import android.content.SharedPreferences

class Conf(
        var sp: SharedPreferences,
        var rootChecked: Boolean,
        var alipayHomeWait: Long,
        var forestHomeWait: Long,
        var friendRankWait: Long,
        var friendHomeWait: Long
) {
    companion object {
        fun read(sp: SharedPreferences): Conf {
            return Conf(
                    sp = sp,
                    rootChecked = sp.getBoolean("root_checked", false),
                    alipayHomeWait = sp.getLong("alipay_home_wait", 1000),
                    forestHomeWait = sp.getLong("forest_home_wait", 1000),
                    friendRankWait = sp.getLong("friend_rank_wait", 1000),
                    friendHomeWait = sp.getLong("friend_home_wait", 1000)
            );
        }
    }

    fun save() {
        sp.edit().putBoolean("root_checked", rootChecked)
                .putLong("alipay_home_wait", alipayHomeWait)
                .putLong("forest_home_wait", forestHomeWait)
                .putLong("friend_rank_wait", friendRankWait)
                .putLong("friend_home_wait", friendHomeWait).apply()
    }

    fun String.hh() {

    }
}