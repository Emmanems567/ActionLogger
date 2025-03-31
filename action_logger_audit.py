import pandas as pd

def read_log_file(file_path):
    log_data = pd.read_csv(file_path, header=None)
    log_data.columns = ["datetime", "dimension", "player", "action", "type", "amount", "x", "y", "z"]
    return log_data

def filter_by_coordinates(log_data):
    print("Ingresa las coordenadas de dos puntos (ejemplo: 608 89 1677):")
    x1, y1, z1 = map(int, input("Punto 1 (x y z): ").split())
    x2, y2, z2 = map(int, input("Punto 2 (x y z): ").split())
    x_min, x_max = sorted([x1, x2])
    y_min, y_max = sorted([y1, y2])
    z_min, z_max = sorted([z1, z2])
    return log_data[
        (log_data["x"] >= x_min) & (log_data["x"] <= x_max) &
        (log_data["y"] >= y_min) & (log_data["y"] <= y_max) &
        (log_data["z"] >= z_min) & (log_data["z"] <= z_max)
    ]

def filter_by_player(log_data):
    player = input("Nombre del jugador: ").strip()
    return log_data[log_data["player"] == player]

def filter_by_action(log_data):
    action = input("Tipo de acción (ej. BREAK, PLACE, OPEN, KILL): ").strip().upper()
    return log_data[log_data["action"] == action]

def filter_by_dimension(log_data):
    dimension = input("Nombre de la dimensión (ej. world, nether, the_end): ").strip().lower()
    return log_data[log_data["dimension"] == dimension]

def count_action_types(log_data):
    counts = log_data["action"].value_counts()
    print("\nCantidad de cada tipo de acción:\n", counts)

def show_events(log_data):
    for _, row in log_data.iterrows():
        print(f"[{row['datetime']}] {row['dimension']} {row['player']} - {row['action']} {row['type']} {row['amount']} - ({row['x']}, {row['y']}, {row['z']})")

def main():
    file_path = "logs.txt"
    
    try:
        log_data = read_log_file(file_path)
    except FileNotFoundError:
        print("El archivo de logs no se encontró.")
        return

    filtered_data = log_data.copy()

    while True:
        print("\n--- Menú ---")
        print("1. Filtrar por coordenadas")
        print("2. Filtrar por jugador")
        print("3. Filtrar por tipo de acción")
        print("4. Filtrar por dimensión")
        print("5. Contar tipos de acciones")
        print("6. Mostrar eventos")
        print("7. Guardar eventos")
        print("8. Restablecer búsqueda")
        print("0. Salir")

        choice = input("Selecciona una opción: ").strip()

        if choice == "1":
            filtered_data = filter_by_coordinates(filtered_data)
            print(f"Se han encontrado {len(filtered_data)} registros")
        elif choice == "2":
            filtered_data = filter_by_player(filtered_data)
            print(f"Se han encontrado {len(filtered_data)} registros")
        elif choice == "3":
            filtered_data = filter_by_action(filtered_data)
            print(f"Se han encontrado {len(filtered_data)} registros")
        elif choice == "5":
            count_action_types(filtered_data)
        elif choice == "6":
            show_events(filtered_data)
        elif choice == "7":
            filtered_data.to_csv("audit_logs.csv", index=False)
            print("Los datos filtrados se han guardado en 'filtered_logs.csv'.")
        elif choice == "8":
            filtered_data = log_data.copy()
            print("Búsqueda restablecida.")
        elif choice == "0":
            print("Saliendo...")
            break
        else:
            print("Opción no válida. Intenta de nuevo.")

if __name__ == "__main__":
    main()