
export function Mover(cols, rows, cellSize) {
    this.cols = cols
    this.rows = rows
    this.cellSize = cellSize
    let dots = new Array(cols * rows)
    for (let i=0; i<dots.length; i++) dots[i] = { side: -1 }
    this.dots = dots
    this.paths = []
    this.moveSide = 0
    this.moveCount = 0
    this.lastDot = null
}

Mover.prototype._setLastDot = function(dot) {
    if (this.lastDot) {
        delete this.lastDot.last
    }
    this.lastDot = dot
    dot.last = true
}

Mover.prototype.move = function(x, y) {
    let dot = this.dots[x * this.rows + y]
    if (dot.side === -1) {
        this._setLastDot(dot)
        dot.side = this.moveSide
        dot.move = this.moveCount++
        this.lookupCaptures(this.moveSide, 2, x, y)
        this.lookupGround(this.moveSide)
        this.moveSide = 1 - this.moveSide
        this.lookupCaptures(this.moveSide, 1, x, y)
        this.lookupGround(this.moveSide)
    } else {
        console.error("Wrong moves")
        debugger
    }
}

Mover.prototype.follow = function(moves) {
    let x, y, lastDot = null
    for(let i=0, len = moves.length; i < len; i+=2) {
        if (moves[i] === '!') {
            // Взятие
            let what = parseInt(moves[i + 1])
            if (what & 1) {
                this.lookupCaptures(1 - this.moveSide, 2, x, y)
            }
            if (what & 2) {
                this.lookupCaptures(this.moveSide, 1, x, y)
            }
            continue
        }
        x = from62(moves[i]);
        y = from62(moves[i + 1]);
        let dot = x >= 0 && x < this.cols && y >= 0 && y < this.rows ? this.dots[x * this.rows + y] : null
        if (dot && dot.side === -1) {
            lastDot = dot
            //moves2.push(moves[i], moves[i + 1])
            dot.side = this.moveSide
            dot.move = this.moveCount++
            this.moveSide = 1 - this.moveSide
        } else {
            console.error("Wrong moves")
            debugger
        }
    }
    if (lastDot) {
        this._setLastDot(lastDot)
    }
    this.lookupGround(this.moveSide)
    this.lookupGround(1 - this.moveSide)
}

Mover.prototype.lookupCaptures = function (side, mode, startX, startY) {
    let dots = this.dots
    let cols = this.cols
    let rows = this.rows
    let captured = false
    const floodX = [0, 1, 0, -1]
    const floodY = [1, 0, -1, 0]
    const floods = 4
    if (startX === 31 && startY === 26) {
        // debugger
    }
    let floodN = 0
    let xMin, xMax, yMin, yMax
    if (mode === 1) {
        xMin = xMax = startX
        yMin = yMax = startY
    } else if (mode === 2) {
        xMin = startX - 1
        xMax = startX + 1
        yMin = startY - 1
        yMax = startY + 1
        if (xMin < 1) {
            xMin = 1
        }
        if (xMax > cols - 2) {
            xMax = cols - 2
        }
        if (yMin < 1) {
            yMin = 1
        }
        if (yMax > rows - 2) {
            yMax = rows - 2
        }
    } else {
        xMin = 1 // 0 пропускаем, т.к. у границы все точки защищены
        xMax = cols - 2
        yMin = 1
        yMax = rows - 2
    }
    for (let x = xMin; x <= xMax; x++) {
        for (let y = yMin; y <= yMax; y++) {
            let dot = dots[x * rows + y]
            if (mode) {
                if (dot._f || dot.side === side || dot.captured) {
                    continue
                }
            } else {
                if (dot._f || dot.side === -1 || dot.side === side || dot.captured) {
                    continue
                }
            }
            // Флудим
            floodN++
            let hasOtherSide = dot.side !== -1 && dot.side !== side
            let captureCount = 0
            let escaped = false
            let capStart = null
            const stack = [x, y, -1]
            dot._f = floodN
            while (true) {
                let nextFloodWay = ++stack[stack.length - 1]
                if (nextFloodWay >= floods) {
                    stack.pop()
                    stack.pop()
                    stack.pop()
                    if (!stack.length) {
                        break
                    }
                    continue
                }
                let lastX = stack[stack.length - 3]
                let lastY = stack[stack.length - 2]
                let nextX = lastX + floodX[nextFloodWay]
                let nextY = lastY + floodY[nextFloodWay]
                if (nextX < 0 || nextX >= cols || nextY < 0 || nextY >= rows) {
                    escaped = true
                    continue
                }
                let nextI = nextX * rows + nextY
                let nextDot = dots[nextI]
                if (!nextDot) {
                    debugger;
                }
                hasOtherSide = hasOtherSide || nextDot.side !== -1 && nextDot.side !== side
                if (nextDot._f > 0) {
                    continue
                }
                if (nextDot.side === side && !nextDot.captured) {
                    nextDot._f = -floodN
                    if (!capStart || nextI < capStart[2]) {
                        capStart = [nextX, nextY, nextI]
                    }
                    continue
                }
                if (nextDot.side !== -1) {
                    captureCount++
                }
                nextDot._f = floodN
                stack.push(nextX)
                stack.push(nextY)
                stack.push(-1)
            }
            if (hasOtherSide && !escaped) {
                captured = true
                for (let x = 0; x < cols; x++) {
                    for (let y = 0; y < rows; y++) {
                        let d = dots[x * rows + y];
                        if (d._f === floodN) {
                            d.captured = true
                            d.capSide = side
                        }
                    }
                }
                const waysX = [-1, -1, 0 , 1 , 1, 1, 0, -1]
                const waysY = [0 , -1, -1, -1, 0, 1, 1,  1]
                const ways = 8
                // 1 2 3
                // 4   6
                // 7 8 9
                const waysD = [4, 7, 8, 9, 6, 3, 2, 1]
                // Попался, нужно теперь построить обводку
                // capStart -- точка с которой начинается обводка
                let startX = capStart[0]
                let startY = capStart[1]
                let curX = startX
                let curY = startY
                let prevWay = 0
                let way = []
                while (true) {
                    // Находим следующую точку пути
                    for (let wi = 0; wi < ways; wi++) {
                        let w = (wi + prevWay + 5) % ways
                        let wx = curX + waysX[w]
                        let wy = curY + waysY[w]
                        if (wx < 0 || wx >= cols || wy < 0 || wy >= rows) {
                            continue
                        }
                        let wDot = dots[wx * rows + wy]
                        if (wDot._f === -floodN) {
                            curX = wx
                            curY = wy
                            if (wDot._fv !== undefined) {
                                way.length = wDot._fv
                            } else {
                                way.push(waysD[w])
                                wDot._fv = way.length
                            }
                            prevWay = w
                            break
                        }
                    }
                    if (curX === startX && curY === startY) {
                        let path = {
                            side,
                            startX,
                            startY,
                            way: way.join(''),
                            move: this.moveCount - 1
                        }
                        this.preparePathWay(path)
                        this.paths.push(path)
                        break
                    }
                }
                for (let x = 0; x < cols; x++) {
                    for (let y = 0; y < rows; y++) {
                        delete dots[x * rows + y]._fv
                    }
                }
            }
        }
    }
    for (let x = 0; x < cols; x++) {
        for (let y = 0; y < rows; y++) {
            delete dots[x * rows + y]._f
        }
    }
    return captured
}

Mover.prototype.preparePathWay = function (path) {
    if (!path.d) {
        let cellSize = this.cellSize
        let d = []
        let w = []
        d.push("M")
        d.push((path.startX + 1) * cellSize - 0.5)
        d.push((this.rows - path.startY) * cellSize - 0.5)
        d.push("l")
        let way = path.way
        for (let i=0; i<way.length; i++) {
            switch (way.charAt(i)) {
                // 1 2 3
                // 4   6
                // 7 8 9

                case '1': w.push('🡼'); d.push(-cellSize, -cellSize); break
                case '2': w.push('🡹'); d.push(0, -cellSize); break
                case '3': w.push('🡽'); d.push(cellSize, -cellSize); break
                case '4': w.push('🡸'); d.push(-cellSize, 0); break
                case '6': w.push('🡺'); d.push(cellSize, 0); break
                case '7': w.push('🡿'); d.push(-cellSize, cellSize); break
                case '8': w.push('🡻'); d.push(0, cellSize); break
                case '9': w.push('🡾'); d.push(cellSize, cellSize); break
            }
        }
        d.push("z")
        path.d = d.join(' ')
        path.w = w.join('')
    }
}

Mover.prototype.lookupGround = function (side) {
    let cols = this.cols
    let rows = this.rows

    // Проверяем по стенкам
    for (let x = 0; x < cols; x++) {
        for (let y = 0; y < rows; y+= rows - 1) {
            let dot = this.dots[x * this.rows + y]
            if (!dot.gnd && (dot.side === side || dot.capSide === side)) {
                this.fillGround(x, y, side)
            }
        }
    }
    for (let x = 0; x < cols; x+= cols -1) {
        for (let y = 0; y < rows; y++) {
            let dot = this.dots[x * this.rows + y]
            if (!dot.gnd && (dot.side === side || dot.capSide === side)) {
                this.fillGround(x, y, side)
            }
        }
    }

    for (let x = 1; x < cols - 1; x++) {
        for (let y = 1; y < rows - 1; y++) {
            let dot = this.dots[x * this.rows + y]
            if (!dot.gnd && (dot.side === side || dot.capSide === side)) {
                dot = this.dots[x * this.rows + y - 1]
                if (dot.gnd && (dot.side === side || dot.capSide === side)) {
                    this.fillGround(x, y, side)
                    continue
                }
                dot = this.dots[x * this.rows + y + 1]
                if (dot.gnd && (dot.side === side || dot.capSide === side)) {
                    this.fillGround(x, y, side)
                    continue
                }
                dot = this.dots[(x - 1) * this.rows + y]
                if (dot.gnd && (dot.side === side || dot.capSide === side)) {
                    this.fillGround(x, y, side)
                    continue
                }
                dot = this.dots[(x + 1) * this.rows + y]
                if (dot.gnd && (dot.side === side || dot.capSide === side)) {
                    this.fillGround(x, y, side)
                    //continue
                }
            }
        }
    }
}

Mover.prototype.fillGround = function (x, y, side) {
    if (x >= 0 && x < this.cols && y >= 0 && y < this.rows) {
        let dot = this.dots[x * this.rows + y]
        if (!dot.gnd && (dot.side === side || dot.capSide === side)) {
            dot.gnd = true
            // delete dot._free
            // delete dot._notGnd
            this.fillGround(x - 1, y, side)
            this.fillGround(x + 1, y, side)
            this.fillGround(x, y - 1, side)
            this.fillGround(x, y + 1, side)
        }
    }
}

Mover.prototype.calcGroundScore = function (side) {
    let cols = this.cols
    let rows = this.rows
    let notGnd = 0
    let free = 0
    for (let x = 1; x < cols - 1; x++) {
        for (let y = 1; y < rows - 1; y++) {
            let dot = this.dots[x * this.rows + y]
            // delete dot._notGnd
            // delete dot._free
            if (dot.gnd) {
                continue
            }
            if (dot.side === side && (!dot.captured || dot.capSide === side)) {
                notGnd ++
                // dot._notGnd = true
            } else if (dot.side !== -1 && dot.side !== side && dot.capSide === side) {
                free ++
                // dot._free = true
            }
        }
    }
    return { notGnd, free }
}

function from62(char) {
    let code = char.charCodeAt(0)
    if (code >= 0x30 && code <= 0x39) {
        return code - 0x30
    } else if (code >= 0x61 && code <= 0x7a) {
        return code - 87
    } else if (code >= 0x41 && code <= 0x5a) {
        return code - 29
    } else {
        throw Error(`Overflow «${char}»`)
    }
}

