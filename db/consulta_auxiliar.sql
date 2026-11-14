SELECT id, nombre, stock FROM producto
WHERE eliminado = FALSE AND stock >= 10
ORDER BY id LIMIT 5;
