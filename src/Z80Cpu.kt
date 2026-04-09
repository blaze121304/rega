class Z80Cpu(private val bus: MemoryBus) {

    // ── 주 레지스터 (8비트) ──────────────────────────────────────────────
    var a = 0; var f = 0
    var b = 0; var c = 0
    var d = 0; var e = 0
    var h = 0; var l = 0

    // ── 섀도 레지스터 ────────────────────────────────────────────────────
    var a2 = 0; var f2 = 0
    var b2 = 0; var c2 = 0
    var d2 = 0; var e2 = 0
    var h2 = 0; var l2 = 0

    // ── 16비트 레지스터 ──────────────────────────────────────────────────
    var pc = 0
    var sp = 0xFFFF
    var ix = 0
    var iy = 0

    // ── 플래그 비트 상수 ─────────────────────────────────────────────────
    companion object {
        const val FLAG_C = 0x01   // Carry
        const val FLAG_N = 0x02   // Add/Subtract
        const val FLAG_PV = 0x04  // Parity/Overflow
        const val FLAG_H = 0x10   // Half Carry
        const val FLAG_Z = 0x40   // Zero
        const val FLAG_S = 0x80   // Sign
    }

    // ── 플래그 헬퍼 ─────────────────────────────────────────────────────
    fun setFlag(mask: Int)   { f = (f or mask) and 0xFF }
    fun clearFlag(mask: Int) { f = (f and mask.inv()) and 0xFF }
    fun testFlag(mask: Int)  = (f and mask) != 0

    private fun updateSZ(value: Int) {
        if (value and 0xFF == 0) setFlag(FLAG_Z) else clearFlag(FLAG_Z)
        if (value and 0x80 != 0) setFlag(FLAG_S) else clearFlag(FLAG_S)
    }

    // ── Fetch ────────────────────────────────────────────────────────────
    private fun fetch(): Int {
        val byte = bus.read(pc) and 0xFF
        pc = (pc + 1) and 0xFFFF
        return byte
    }

    // ── Fetch-Decode-Execute (1 명령어 실행, T-State 반환) ──────────────
    fun step(): Int {
        val opcode = fetch()
        return when (opcode) {

            // NOP
            0x00 -> 4

            // LD r, n  (즉시값 로드)
            0x3E -> { a = fetch(); 7 }          // LD A, n
            0x06 -> { b = fetch(); 7 }          // LD B, n
            0x0E -> { c = fetch(); 7 }          // LD C, n
            0x16 -> { d = fetch(); 7 }          // LD D, n
            0x1E -> { e = fetch(); 7 }          // LD E, n
            0x26 -> { h = fetch(); 7 }          // LD H, n
            0x2E -> { l = fetch(); 7 }          // LD L, n

            // ADD A, r
            0x80 -> { addA(b); 4 }              // ADD A, B
            0x81 -> { addA(c); 4 }              // ADD A, C
            0x82 -> { addA(d); 4 }              // ADD A, D
            0x83 -> { addA(e); 4 }              // ADD A, E
            0x84 -> { addA(h); 4 }              // ADD A, H
            0x85 -> { addA(l); 4 }              // ADD A, L
            0x87 -> { addA(a); 4 }              // ADD A, A

            // ADD A, n (즉시값 덧셈)
            0xC6 -> { addA(fetch()); 7 }        // ADD A, n

            else -> throw IllegalStateException("Unknown opcode: 0x${opcode.toString(16).uppercase()}")
        }
    }

    // ── ADD A, src 연산 + 플래그 갱신 ───────────────────────────────────
    private fun addA(src: Int) {
        val result = a + (src and 0xFF)
        val halfCarry = (a and 0x0F) + (src and 0x0F) > 0x0F

        clearFlag(FLAG_N)
        if (result > 0xFF) setFlag(FLAG_C) else clearFlag(FLAG_C)
        if (halfCarry)     setFlag(FLAG_H) else clearFlag(FLAG_H)

        // Overflow: 두 양수를 더했는데 음수가 나오거나, 두 음수를 더했는데 양수가 나온 경우
        val overflow = ((a xor src.inv()) and (a xor result) and 0x80) != 0
        if (overflow) setFlag(FLAG_PV) else clearFlag(FLAG_PV)

        a = result and 0xFF
        updateSZ(a)
    }
}
