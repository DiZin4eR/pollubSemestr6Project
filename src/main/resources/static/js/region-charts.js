(function () {
    const cards = document.querySelectorAll(".region-card");

    cards.forEach((card) => {
        const canvas = card.querySelector(".region-chart");
        const rows = Array.from(card.querySelectorAll("tbody tr[data-year][data-value]"));

        if (!canvas || rows.length === 0) {
            return;
        }

        const points = rows
                .map((row) => ({
                    year: Number.parseInt(row.dataset.year, 10),
                    value: parseNumber(row.dataset.value)
                }))
                .filter((point) => Number.isFinite(point.year) && Number.isFinite(point.value))
                .sort((first, second) => first.year - second.year);

        if (points.length === 0) {
            return;
        }

        drawLineChart(canvas, points);
    });

    function parseNumber(value) {
        if (!value) {
            return Number.NaN;
        }

        return Number.parseFloat(value.replace(/\s/g, "").replace(",", "."));
    }

    function drawLineChart(canvas, points) {
        const context = canvas.getContext("2d");
        const width = canvas.clientWidth;
        const height = canvas.height;
        const scale = window.devicePixelRatio || 1;

        canvas.width = Math.floor(width * scale);
        canvas.height = Math.floor(height * scale);
        context.scale(scale, scale);

        const padding = {
            top: 18,
            right: 18,
            bottom: 34,
            left: 56
        };
        const chartWidth = width - padding.left - padding.right;
        const chartHeight = height - padding.top - padding.bottom;
        const years = points.map((point) => point.year);
        const values = points.map((point) => point.value);
        const minYear = Math.min(...years);
        const maxYear = Math.max(...years);
        const minValue = Math.min(...values);
        const maxValue = Math.max(...values);
        const valueRange = maxValue - minValue || 1;
        const yearRange = maxYear - minYear || 1;

        context.clearRect(0, 0, width, height);
        context.font = "12px Inter, system-ui, sans-serif";
        context.lineWidth = 1;
        context.strokeStyle = "#d9e0e8";
        context.fillStyle = "#607080";

        for (let index = 0; index <= 3; index++) {
            const y = padding.top + (chartHeight / 3) * index;
            context.beginPath();
            context.moveTo(padding.left, y);
            context.lineTo(width - padding.right, y);
            context.stroke();

            const labelValue = maxValue - (valueRange / 3) * index;
            context.fillText(formatNumber(labelValue), 8, y + 4);
        }

        context.strokeStyle = "#0f766e";
        context.lineWidth = 2.5;
        context.beginPath();

        points.forEach((point, index) => {
            const x = padding.left + ((point.year - minYear) / yearRange) * chartWidth;
            const y = padding.top + chartHeight - ((point.value - minValue) / valueRange) * chartHeight;

            if (index === 0) {
                context.moveTo(x, y);
            } else {
                context.lineTo(x, y);
            }
        });

        context.stroke();

        context.fillStyle = "#115e59";
        points.forEach((point) => {
            const x = padding.left + ((point.year - minYear) / yearRange) * chartWidth;
            const y = padding.top + chartHeight - ((point.value - minValue) / valueRange) * chartHeight;

            context.beginPath();
            context.arc(x, y, 3, 0, Math.PI * 2);
            context.fill();
        });

        context.fillStyle = "#607080";
        context.fillText(String(minYear), padding.left, height - 10);
        context.fillText(String(maxYear), width - padding.right - 34, height - 10);
    }

    function formatNumber(value) {
        return new Intl.NumberFormat("pl-PL", {
            maximumFractionDigits: 2
        }).format(value);
    }
})();
