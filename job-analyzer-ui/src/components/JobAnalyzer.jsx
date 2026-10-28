import { useState } from "react";
import "../styles/JobAnalyzer.css";

function JobAnalyzer() {
    const [resume, setResume] = useState(null);
    const [jobDescription, setJobDescription] = useState("");
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState(null);
    const [error, setError] = useState("");

    const handleResumeChange = (event) => {
        setResume(event.target.files[0]);
    };

    const handleAnalyze = async (event) => {
        event.preventDefault();
        setError("");
        setResult(null);

        if (!resume) {
            setError("Please select a resume.");
            return;
        }

        if (!jobDescription.trim()) {
            setError("Please enter a job description.");
            return;
        }

        const formData = new FormData();
        formData.append("resume", resume);
        formData.append("jobDescription", jobDescription);

        const token = localStorage.getItem("token");

        setLoading(true);
        try {
            const response = await fetch("http://localhost:8080/jobs/analyze", {
                method: "POST",
                headers: { Authorization: `Bearer ${token}` },
                body: formData,
            });

            if (response.status === 401) {
                localStorage.removeItem("token");
                setError("Session expired. Please log in again.");
                return;
            }

            if (!response.ok) {
                setError("Failed to analyze the resume. Please try again.");
                return;
            }

            const data = await response.json();
            setResult(data);
        } catch (err) {
            setError("Cannot reach server. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="analyzer-page">
            <div className="analyzer-container">
                <div className="analyzer-header">
                    <h1 className="analyzer-title">Job Analyzer</h1>
                    <p className="analyzer-subtitle">
                        Upload your resume and paste a job description to get an instant analysis.
                    </p>
                </div>

                <div className="analyzer-card">
                    <form onSubmit={handleAnalyze} className="analyzer-form">
                        <div className="analyzer-field">
                            <label className="analyzer-label">Upload Resume (PDF)</label>
                            <input
                                type="file"
                                accept=".pdf"
                                onChange={handleResumeChange}
                                className="analyzer-file-input"
                            />
                        </div>

                        <div className="analyzer-field">
                            <label className="analyzer-label">Job Description</label>
                            <textarea
                                className="analyzer-textarea"
                                value={jobDescription}
                                onChange={(e) => setJobDescription(e.target.value)}
                                placeholder="Paste the job description here..."
                            />
                        </div>

                        {error && <div className="analyzer-error">{error}</div>}

                        <button
                            type="submit"
                            className="analyzer-button"
                            disabled={loading}
                        >
                            {loading ? "Analyzing..." : "Analyze Resume"}
                        </button>
                    </form>
                </div>

                {result && (
                    <div className="result-card">
                        <div className="result-header">
                            <h2 className="result-title">Analysis Result</h2>
                            {result.level && (
                                <span className="result-badge">{result.level}</span>
                            )}
                        </div>

                        <div className="result-meta">
                            <div className="result-meta-item">
                                <div className="result-meta-label">Level</div>
                                <div className="result-meta-value">
                                    {result.level || "—"}
                                </div>
                            </div>
                            <div className="result-meta-item">
                                <div className="result-meta-label">Experience</div>
                                <div className="result-meta-value">
                                    {result.experienceYears || "—"}
                                </div>
                            </div>
                        </div>

                        {result.skills && result.skills.length > 0 && (
                            <div className="result-section">
                                <h3 className="result-section-title">Skills</h3>
                                <div className="skills-list">
                                    {result.skills.map((skill, i) => (
                                        <span key={i} className="skill-chip">
                                            {skill}
                                        </span>
                                    ))}
                                </div>
                            </div>
                        )}

                        {result.summary && (
                            <div className="result-section">
                                <h3 className="result-section-title">Summary</h3>
                                <p className="result-summary">{result.summary}</p>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}

export default JobAnalyzer;