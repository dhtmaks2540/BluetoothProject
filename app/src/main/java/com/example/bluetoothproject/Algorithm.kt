package com.example.bluetoothproject

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

fun main() = with(BufferedReader(InputStreamReader(System.`in`))) {
    var st = StringTokenizer(readLine(), " ")
    // 사다리의 수, 뱀의 수
    val n = st.nextToken().toInt()
    val m = st.nextToken().toInt()

    val graph = Array(10) { IntArray(10) }
    val ladderList = Array(n) { IntArray(2) }
    val snakeList = Array(m) { IntArray(2) }

    // 사다리 리스트
    repeat(n) { index ->
        st = StringTokenizer(readLine(), " ")
        val x = st.nextToken().toInt()
        val y = st.nextToken().toInt()
        ladderList[index][0] = x
        ladderList[index][1] = y
    }

    // 뱀 리스트
    repeat(m) { index ->
        st = StringTokenizer(readLine(), " ")
        val x = st.nextToken().toInt()
        val y = st.nextToken().toInt()
        snakeList[index][0] = x
        snakeList[index][1] = y
    }

    var cnt = 1

    // 그래프에 숫자 표시
    for(x in 0..9) {
        for(y in 0..9) {
            graph[x][y] = cnt++
        }
    }

    // 큐 선언 및 시작점
    val queue = ArrayDeque<Int>()
    queue.add(1)

    while(queue.isNotEmpty()) {
        for(i in queue.indices) {
            val now = queue.pollFirst()
        }
    }
}