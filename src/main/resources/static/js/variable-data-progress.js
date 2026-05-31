(function () {
    const tracker = document.querySelector("[data-variable-data-job]");

    if (!tracker) {
        return;
    }

    const jobId = tracker.dataset.jobId;
    const variableId = tracker.dataset.variableId;
    const fill = tracker.querySelector("[data-progress-fill]");
    const label = tracker.querySelector("[data-progress-label]");
    const detail = tracker.querySelector("[data-progress-detail]");

    if (!jobId || !variableId) {
        return;
    }

    poll();

    function poll() {
        fetch(`/api/gus/variable-data-jobs/${encodeURIComponent(jobId)}`, {
            headers: {
                "Accept": "application/json"
            }
        })
                .then((response) => {
                    if (!response.ok) {
                        throw new Error(`Progress request failed with status ${response.status}`);
                    }

                    return response.json();
                })
                .then(updateProgress)
                .catch((error) => {
                    setDetail(error.message || "Could not read progress");
                    window.setTimeout(poll, 3000);
                });
    }

    function updateProgress(job) {
        const percent = clamp(job.progressPercent || 0, 0, 100);

        if (fill) {
            fill.style.width = `${percent}%`;
        }

        if (label) {
            label.textContent = `${percent}%`;
        }

        setDetail(job.message || "Fetching GUS data");

        if (job.state === "completed") {
            reloadWithJobId(job.id);
            return;
        }

        if (job.state === "failed") {
            tracker.classList.add("progress-card-error");
            setDetail(job.message || "GUS request failed");
            return;
        }

        window.setTimeout(poll, 1500);
    }

    function reloadWithJobId(id) {
        const url = new URL(window.location.href);

        url.searchParams.set("jobId", id);
        window.location.replace(url.toString());
    }

    function setDetail(value) {
        if (detail) {
            detail.textContent = value;
        }
    }

    function clamp(value, min, max) {
        return Math.min(max, Math.max(min, value));
    }
})();
