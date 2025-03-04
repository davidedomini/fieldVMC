import os
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt

# Imposta lo stile di Seaborn e la palette viridis
sns.set_theme(style="darkgrid")
sns.set_palette("viridis")

# Percorsi delle cartelle
BASE_PATH = ""
CLEANED_PATH = os.path.join(BASE_PATH, "data", "cleaned")
OUTPUT_PATH = os.path.join(BASE_PATH, "charts")

# Creazione della cartella charts se non esiste
os.makedirs(OUTPUT_PATH, exist_ok=True)

def plot_experiment(file_path):
    """Legge il CSV e genera un grafico per l'esperimento."""
    df = pd.read_csv(file_path)

    # La prima colonna è considerata il tempo
    time_col = df.columns[0]

    # Nome dell'esperimento estratto dal file
    experiment_name = os.path.basename(file_path).replace("cleaned_", "").replace(".csv", "")

    # Creazione della figura
    plt.figure(figsize=(10, 6))
    for col in df.columns[1:]:  # Esclude la colonna del tempo
        sns.lineplot(x=df[time_col], y=df[col], label=col)

    plt.title(f"Experiment: {experiment_name}")
    plt.xlabel("Time")
    plt.ylabel("Values")
    plt.legend(title="Metrics")

    # Salvataggio in alta qualità
    output_file = os.path.join(OUTPUT_PATH, f"{experiment_name}.svg")
    plt.savefig(output_file, format="svg", bbox_inches="tight")
    plt.close()
    print(f"Saved: {output_file}")

def plot_graphs_for_each_column(input_file, output_dir, experiment_name):
    # Leggi il file CSV
    df = pd.read_csv(input_file, sep=" ", header=0)

    # Crea la cartella di output se non esiste
    os.makedirs(output_dir, exist_ok=True)

    # Itera su tutte le colonne del dataframe (eccetto 'time' che non va plottato)
    for column in df.columns[1:]:
        plt.figure(figsize=(10, 6))

        # Crea il grafico per la colonna
        sns.lineplot(x=df['time'], y=df[column], palette="viridis")

        # Imposta il titolo e le etichette degli assi
        plt.title(f"Graph for {experiment_name} - {column}")
        plt.xlabel('Time')
        plt.ylabel(column)

        # Salva il grafico in PDF o SVG
        output_file_pdf = os.path.join(output_dir, f"{experiment_name}_{column}.pdf")
        output_file_svg = os.path.join(output_dir, f"{experiment_name}_{column}.svg")
        plt.savefig(output_file_pdf, format="pdf", dpi=300)
        plt.savefig(output_file_svg, format="svg", dpi=300)

        # Chiude il grafico per non sovrascrivere
        plt.close()

def main():
    """Trova e plotta tutti gli esperimenti nella cartella cleaned."""
    files = [f for f in os.listdir(CLEANED_PATH) if f.startswith("cleaned_") and f.endswith(".csv")]

    if not files:
        print("No cleaned experiment files found.")
        return

    for file in files:
        file_path = os.path.join(CLEANED_PATH, file)
        plot_graphs_for_each_column(file_path, "charts", file)

if __name__ == "__main__":
    main()
