import sharp from "sharp";

const entrada = "public/images/login-construccion.jpg";
const salida = "public/images/login-construccion-recortada.jpg";

const { width, height } = await sharp(entrada).metadata();

// Conserva el 88% de arriba, quita el 12% de abajo
const nuevaAltura = Math.round(height * 0.88);

await sharp(entrada)
  .extract({ left: 0, top: 0, width, height: nuevaAltura })
  .toFile(salida);

console.log(`Listo: ${width}x${height} -> ${width}x${nuevaAltura}`);