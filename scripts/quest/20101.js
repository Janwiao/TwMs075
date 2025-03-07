﻿/* 
 * Cygnus 1st Job advancement - Soul 
 */

var status = -1;

function end(mode, type, selection) {
    if (mode == 0) {
	if (status == 0) {
	    qm.sendNext("這個決定..非常重要.");
	    qm.safeDispose();
	    return;
	}
	status--;
    } else {
	status++;
    }
    if (status == 0) {
	qm.sendYesNo("你決定好了嘛? 這會是你最後的決定唷, 所以想清楚你要做什麼. 你想要成為 聖魂騎士嘛?");
    } else if (status == 1) {
	qm.sendNext("恭喜成功轉職。");
	if (qm.getJob() != 1100) {
	    qm.gainItem(1302077, 1);
	    qm.gainItem(1142066, 1);
		qm.resetStats(4, 4, 4, 4);
	    qm.expandInventory(1, 4);
	    qm.expandInventory(4, 4);
	    qm.changeJob(1100);
	}
	qm.forceCompleteQuest();
    } else if (status == 2) {
	qm.sendNextPrev("我還擴充您身上的裝備欄空間");
    } else if (status == 3) {
	qm.sendNextPrev("好運！.");
	qm.safeDispose();
    }
}