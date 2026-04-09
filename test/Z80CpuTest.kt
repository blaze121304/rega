import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class Z80CpuTest {

    private lateinit var mem: FlatMemory
    private lateinit var cpu: Z80Cpu

    @BeforeEach
    fun setup() {
        mem = FlatMemory()
        cpu = Z80Cpu(mem)
    }

    /** 메모리 주소 0번지부터 바이트 배열을 올려놓는 헬퍼 */
    private fun load(vararg bytes: Int) {
        bytes.forEachIndexed { i, b -> mem.write(i, b) }
        cpu.pc = 0
    }

    // ── Test 1: LD A, 10 → A == 10 ──────────────────────────────────────
    @Test
    fun `LD A,n 실행 후 A 레지스터에 값이 로드된다`() {
        load(0x3E, 10)   // LD A, 10
        cpu.step()
        assertEquals(10, cpu.a)
    }

    // ── Test 2: LD A,5 / LD B,3 / ADD A,B → A == 8 ──────────────────────
    @Test
    fun `ADD A,B 실행 후 A에 합산 결과가 저장된다`() {
        load(
            0x3E, 5,   // LD A, 5
            0x06, 3,   // LD B, 3
            0x80       // ADD A, B
        )
        repeat(3) { cpu.step() }
        assertEquals(8, cpu.a)
        assertFalse(cpu.testFlag(Z80Cpu.FLAG_C), "Carry는 서면 안 된다")
    }

    // ── Test 3: 0xFF + 1 → A == 0, Carry 서야 함 ────────────────────────
    @Test
    fun `0xFF에 1을 더하면 A는 0이 되고 Carry 플래그가 선다`() {
        load(
            0x3E, 0xFF, // LD A, 255
            0x06, 1,    // LD B, 1
            0x80        // ADD A, B
        )
        repeat(3) { cpu.step() }
        assertEquals(0, cpu.a, "A는 0이어야 한다")
        assertTrue(cpu.testFlag(Z80Cpu.FLAG_C), "Carry 플래그가 서야 한다")
        assertTrue(cpu.testFlag(Z80Cpu.FLAG_Z), "Zero 플래그가 서야 한다")
    }

    // ── Test 4: NOP은 PC를 1 증가시키고 4 T-State를 소비한다 ─────────────
    @Test
    fun `NOP은 PC를 1 증가시키고 4 T-State를 반환한다`() {
        load(0x00)
        val cycles = cpu.step()
        assertEquals(1, cpu.pc)
        assertEquals(4, cycles)
    }

    // ── Test 5: ADD A,n (즉시값) ─────────────────────────────────────────
    @Test
    fun `ADD A,n 즉시값 덧셈이 정확히 동작한다`() {
        load(
            0x3E, 20,   // LD A, 20
            0xC6, 22    // ADD A, 22
        )
        repeat(2) { cpu.step() }
        assertEquals(42, cpu.a)
    }

    // ── Test 6: LD B/C/D/E/H/L 각각 즉시값 로드 ────────────────────────
    @Test
    fun `LD 계열 명령어가 각 레지스터에 값을 정확히 로드한다`() {
        load(
            0x06, 1,  // LD B, 1
            0x0E, 2,  // LD C, 2
            0x16, 3,  // LD D, 3
            0x1E, 4,  // LD E, 4
            0x26, 5,  // LD H, 5
            0x2E, 6   // LD L, 6
        )
        repeat(6) { cpu.step() }
        assertEquals(1, cpu.b)
        assertEquals(2, cpu.c)
        assertEquals(3, cpu.d)
        assertEquals(4, cpu.e)
        assertEquals(5, cpu.h)
        assertEquals(6, cpu.l)
    }

    // ── Test 7: Overflow 플래그 — 양수 + 양수 = 음수 ────────────────────
    @Test
    fun `양수 오버플로우 발생 시 PV 플래그가 선다`() {
        load(
            0x3E, 0x7F, // LD A, 127
            0x06, 0x01, // LD B, 1
            0x80        // ADD A, B  → 128 (0x80) = 음수로 해석 → overflow
        )
        repeat(3) { cpu.step() }
        assertEquals(0x80, cpu.a)
        assertTrue(cpu.testFlag(Z80Cpu.FLAG_PV), "Overflow 플래그가 서야 한다")
        assertTrue(cpu.testFlag(Z80Cpu.FLAG_S),  "Sign 플래그가 서야 한다")
    }
}
