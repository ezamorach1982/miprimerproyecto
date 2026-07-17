class Calculator {
  constructor(previousOperandElement, currentOperandElement) {
    this.previousOperandElement = previousOperandElement;
    this.currentOperandElement = currentOperandElement;
    this.clear();
  }

  clear() {
    this.currentOperand = "0";
    this.previousOperand = "";
    this.operator = undefined;
  }

  delete() {
    this.currentOperand = this.currentOperand.slice(0, -1) || "0";
  }

  appendNumber(number) {
    if (number === "." && this.currentOperand.includes(".")) return;
    if (this.currentOperand === "0" && number !== ".") {
      this.currentOperand = number;
    } else {
      this.currentOperand += number;
    }
  }

  chooseOperator(operator) {
    if (this.currentOperand === "0" && this.previousOperand === "") return;

    if (this.previousOperand !== "") {
      this.compute();
    }

    this.operator = operator;
    this.previousOperand = this.currentOperand;
    this.currentOperand = "0";
  }

  percent() {
    this.currentOperand = (parseFloat(this.currentOperand) / 100).toString();
  }

  compute() {
    let result;
    const prev = parseFloat(this.previousOperand);
    const current = parseFloat(this.currentOperand);

    if (isNaN(prev) || isNaN(current)) return;

    switch (this.operator) {
      case "+":
        result = prev + current;
        break;
      case "-":
        result = prev - current;
        break;
      case "*":
        result = prev * current;
        break;
      case "/":
        result = current === 0 ? "Error" : prev / current;
        break;
      default:
        return;
    }

    this.currentOperand = result.toString();
    this.operator = undefined;
    this.previousOperand = "";
  }

  formatNumber(value) {
    if (value === "Error") return value;
    const [integerPart, decimalPart] = value.split(".");
    const formattedInteger = new Intl.NumberFormat("es-ES").format(
      parseFloat(integerPart)
    );
    return decimalPart !== undefined
      ? `${formattedInteger},${decimalPart}`
      : formattedInteger;
  }

  updateDisplay() {
    this.currentOperandElement.textContent = this.formatNumber(
      this.currentOperand
    );
    this.previousOperandElement.textContent = this.operator
      ? `${this.formatNumber(this.previousOperand)} ${this.operator}`
      : "";
  }
}

const previousOperandElement = document.getElementById("previousOperand");
const currentOperandElement = document.getElementById("currentOperand");
const calculator = new Calculator(previousOperandElement, currentOperandElement);

document.querySelectorAll("[data-number]").forEach((button) => {
  button.addEventListener("click", () => {
    calculator.appendNumber(button.dataset.number);
    calculator.updateDisplay();
  });
});

document.querySelectorAll("[data-operator]").forEach((button) => {
  button.addEventListener("click", () => {
    calculator.chooseOperator(button.dataset.operator);
    calculator.updateDisplay();
  });
});

document.querySelectorAll("[data-action]").forEach((button) => {
  button.addEventListener("click", () => {
    switch (button.dataset.action) {
      case "clear":
        calculator.clear();
        break;
      case "delete":
        calculator.delete();
        break;
      case "percent":
        calculator.percent();
        break;
      case "equals":
        calculator.compute();
        break;
    }
    calculator.updateDisplay();
  });
});

document.addEventListener("keydown", (event) => {
  if (event.key >= "0" && event.key <= "9") {
    calculator.appendNumber(event.key);
  } else if (event.key === ".") {
    calculator.appendNumber(".");
  } else if (["+", "-", "*", "/"].includes(event.key)) {
    calculator.chooseOperator(event.key);
  } else if (event.key === "Enter" || event.key === "=") {
    event.preventDefault();
    calculator.compute();
  } else if (event.key === "Backspace") {
    calculator.delete();
  } else if (event.key === "Escape") {
    calculator.clear();
  } else {
    return;
  }
  calculator.updateDisplay();
});
