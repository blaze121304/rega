interface MemoryBus {
    fun read(address: Int): Int
    fun write(address: Int, value: Int)
}

class FlatMemory(size: Int = 0x10000) : MemoryBus {
    private val ram = IntArray(size)

    override fun read(address: Int): Int = ram[address and 0xFFFF]

    override fun write(address: Int, value: Int) {
        ram[address and 0xFFFF] = value and 0xFF
    }
}
