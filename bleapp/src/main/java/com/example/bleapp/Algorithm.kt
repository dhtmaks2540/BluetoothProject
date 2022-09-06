package com.example.bleapp

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import kotlin.math.min

fun main() = with(BufferedReader(InputStreamReader(System.`in`))) {
    val st = StringTokenizer(readLine(), " ")
    val n = st.nextToken().toInt()
    val m = st.nextToken().toInt()

    val graph = Array(n) { IntArray(m) }

    repeat(n) { row ->
        val str = readLine()
        repeat(m) { col ->
            graph[row][col] = str[col].digitToInt()
        }
    }

    val dxy = arrayOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
    var breakWallFlag = false
    var cnt = Int.MAX_VALUE

    fun dfs(x: Int, y: Int, nowCnt: Int) {
        // 목표 위치에 도달했다면
        if(x == n - 1 && y == m - 1) {
            cnt = min(cnt, nowCnt)
        }

        for(i in 0..3) {
            val nx = x + dxy[i].first
            val ny = y + dxy[i].second

            // 범위를 벗어나면 패스
            if(nx < 0 || nx >= n || ny < 0 || ny >= m) {
                continue
            }

            // 이미 방문했다면 패스
            if(graph[nx][ny] == 2) {
                continue
            }

            // 현재 위치가 벽이고 아직 벽을 부순적이 없다면 부수고 들어가기
            if(graph[nx][ny] == 1 && !breakWallFlag) {
                graph[nx][ny] = 2
                breakWallFlag = true
                dfs(nx, ny, nowCnt + 2)
                graph[nx][ny] = 1
                breakWallFlag = false
            } else if(graph[nx][ny] == 0) { // 현재 위치가 빈 공간이라면 들어가기
                graph[nx][ny] = 2
                dfs(nx, ny, nowCnt + 1)
                graph[nx][ny] = 0
            }
        }
    }

    dfs(0, 0, 0)

    if(cnt == Int.MAX_VALUE) {
        println(-1)
    } else {
        println(cnt)
    }
}