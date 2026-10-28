package net.gera.veridia.common.village.calc;
 /** Клетка сетки скана (VillageScan) - НЕ мировые координаты: мировые получаются через
 * scan.worldX(x) / scan.worldZ(z). Record, чтобы её можно было класть в HashSet/HashMap как есть,
 * без упаковки в long-ключ.*/
public record Cell(int x, int z){}
//Very important