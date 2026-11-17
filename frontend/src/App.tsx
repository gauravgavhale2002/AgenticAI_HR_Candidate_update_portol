import { useEffect, useState, type FormEvent } from 'react'
import { ArrowLeft, BriefcaseBusiness, CheckCircle2, RefreshCw, ShieldCheck, Sparkles, Trash2, Users } from 'lucide-react'
import { getCountries, getCountryCallingCode, isValidPhoneNumber, parsePhoneNumber } from 'libphonenumber-js/max'

type RegisteredCandidate = {
  id: number
  firstName: string
  lastName: string
  email: string
  phone: string | null
  registeredAt: string
  currentDesignation: string | null
  experienceYears: number | null
  skills: string[]
  resumeId: number | null
  resumeFileName: string | null
  cvSkills: string[]
  cvExperienceYears: number | null
  resumeScreeningScore: number | null
  resumeScreeningSummary: string | null
  hiringStatus: HiringStatus
  hiringStatusUpdatedAt: string | null
  managerFeedback: string | null
  jobMatches: {
    jobId: number
    title: string
    matchScore: number
    matchedSkills: string[]
    missingSkills: string[]
  }[]
}

type HiringStatus =
  | 'IN_REVIEW'
  | 'SELECTED_FOR_FIRST_ROUND'
  | 'SELECTED_FOR_MANAGER_ROUND'
  | 'SELECTED'
  | 'OFFER_RELEASED'
  | 'REJECTED'

type CandidatePortal = {
  profile: {
    id: number
    firstName: string
    lastName: string
    email: string
    currentDesignation: string | null
    experienceYears: number | null
    skills: string[]
    hiringStatus: HiringStatus
  }
  statusHistory: {
    status: HiringStatus
    changedAt: string
  }[]
}

const hiringStages: { status: HiringStatus; label: string }[] = [
  { status: 'IN_REVIEW', label: 'In review' },
  { status: 'SELECTED_FOR_FIRST_ROUND', label: '1st round' },
  { status: 'SELECTED_FOR_MANAGER_ROUND', label: 'Manager round' },
  { status: 'SELECTED', label: 'Selected' },
  { status: 'OFFER_RELEASED', label: 'Offer released' },
]

const phoneCountries = getCountries()
  .map((code) => ({
    code,
    name: new Intl.DisplayNames(['en'], { type: 'region' }).of(code) ?? code,
    callingCode: getCountryCallingCode(code),
  }))
  .sort((first, second) => first.name.localeCompare(second.name))

function getStageLabel(status: HiringStatus) {
  return status === 'REJECTED' ? 'Rejected' : hiringStages.find((stage) => stage.status === status)?.label ?? 'Unknown'
}

function getNextHiringStatus(status: HiringStatus): HiringStatus | null {
  const index = hiringStages.findIndex((stage) => stage.status === status)
  return index >= 0 && index < hiringStages.length - 1 ? hiringStages[index + 1].status : null
}

function RefreshButton({ onClick, busy = false, label = 'Refresh page' }: {
  onClick: () => void
  busy?: boolean
  label?: string
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={busy}
      aria-label={label}
      title={label}
      className="inline-flex items-center gap-2 rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-300 transition hover:border-indigo-400 hover:text-white disabled:cursor-not-allowed disabled:opacity-60"
    >
      <RefreshCw className={`h-4 w-4 ${busy ? 'animate-spin' : ''}`} />
      {busy ? 'Refreshing…' : 'Refresh'}
    </button>
  )
}

function HiringProgress({ status }: { status: HiringStatus }) {
  if (status === 'REJECTED') {
    return (
      <div className="rounded-xl border border-rose-500/30 bg-rose-500/10 p-4 text-rose-200">
        <p className="font-semibold">Application closed</p>
        <p className="mt-1 text-sm text-rose-200/80">Your application will not move to another interview stage.</p>
      </div>
    )
  }

  const activeIndex = hiringStages.findIndex((stage) => stage.status === status)
  const completed = status === 'OFFER_RELEASED'
  return (
    <ol className="grid gap-3 sm:grid-cols-5" aria-label="Hiring progress">
      {hiringStages.map((stage, index) => {
        const complete = completed || index < activeIndex
        const active = !completed && index === activeIndex
        return (
          <li key={stage.status} className="flex items-start gap-3 sm:flex-col sm:gap-2">
            <span className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full border text-sm font-semibold ${
              complete ? 'border-emerald-400 bg-emerald-400 text-slate-950'
                : active ? 'border-amber-300 bg-amber-300 text-slate-950'
                  : 'border-slate-600 text-slate-400'
            }`}>
              {complete ? <CheckCircle2 className="h-5 w-5" /> : index + 1}
            </span>
            <span className={`pt-1 text-sm ${complete ? 'text-emerald-300' : active ? 'font-semibold text-amber-200' : 'text-slate-400'}`}>
              {stage.label}
              {active && <span className="ml-1 text-xs">(in progress)</span>}
            </span>
          </li>
        )
      })}
    </ol>
  )
}

const highlights = [
  { icon: BriefcaseBusiness, title: 'AI-powered hiring', text: 'Resume intelligence and role-fit scoring to improve recruiting quality.' },
  { icon: ShieldCheck, title: 'Secure workflows', text: 'Role-based access, controlled AI actions, and governed candidate handling.' },
  { icon: CheckCircle2, title: 'Lifecycle visibility', text: 'Complete recruitment tracking from registration to offer and onboarding.' },
]

export default function App() {
  const [showRegistration, setShowRegistration] = useState(() => window.location.pathname === '/register')
  const [showHrDashboard, setShowHrDashboard] = useState(() => window.location.pathname === '/hr')
  const [showCandidatePortal, setShowCandidatePortal] = useState(() => window.location.pathname === '/profile')
  const [registrationError, setRegistrationError] = useState('')
  const [registrationComplete, setRegistrationComplete] = useState(false)
  const [candidateEmail, setCandidateEmail] = useState('')
  const [candidatePassword, setCandidatePassword] = useState('')
  const [candidatePortal, setCandidatePortal] = useState<CandidatePortal | null>(null)
  const [candidatePortalError, setCandidatePortalError] = useState('')
  const [isRefreshingCandidatePortal, setIsRefreshingCandidatePortal] = useState(false)
  const [isRefreshingHrCandidates, setIsRefreshingHrCandidates] = useState(false)
  const [outcomePopup, setOutcomePopup] = useState<'REJECTED' | 'OFFER_RELEASED' | null>(null)
  const [hrUsername, setHrUsername] = useState('')
  const [hrPassword, setHrPassword] = useState('')
  const [hrError, setHrError] = useState('')
  const [candidates, setCandidates] = useState<RegisteredCandidate[] | null>(null)
  const [selectedCandidate, setSelectedCandidate] = useState<RegisteredCandidate | null>(null)
  const [deletingCandidateId, setDeletingCandidateId] = useState<number | null>(null)
  const [hrNotes, setHrNotes] = useState('')
  const [managerFeedback, setManagerFeedback] = useState('')
  const [resumePreview, setResumePreview] = useState<{ url: string; fileName: string } | null>(null)

  useEffect(() => {
    const syncRoute = () => {
      setShowRegistration(window.location.pathname === '/register')
      setShowHrDashboard(window.location.pathname === '/hr')
      setShowCandidatePortal(window.location.pathname === '/profile')
    }
    window.addEventListener('popstate', syncRoute)
    return () => window.removeEventListener('popstate', syncRoute)
  }, [])

  function openRegistration() {
    window.history.pushState({}, '', '/register')
    setShowRegistration(true)
    setShowHrDashboard(false)
    setShowCandidatePortal(false)
  }

  function closeRegistration() {
    window.history.pushState({}, '', '/')
    setShowRegistration(false)
    setShowCandidatePortal(false)
    setRegistrationComplete(false)
  }

  function openHrDashboard() {
    window.history.pushState({}, '', '/hr')
    setShowRegistration(false)
    setShowHrDashboard(true)
    setShowCandidatePortal(false)
  }

  function closeHrDashboard() {
    window.history.pushState({}, '', '/')
    setShowHrDashboard(false)
    setShowCandidatePortal(false)
    setCandidates(null)
    setHrError('')
  }

  function openCandidatePortal() {
    window.history.pushState({}, '', '/profile')
    setShowRegistration(false)
    setShowHrDashboard(false)
    setShowCandidatePortal(true)
    setCandidatePortalError('')
  }

  function closeCandidatePortal() {
    window.history.pushState({}, '', '/')
    setShowCandidatePortal(false)
    setCandidatePortal(null)
    setCandidateEmail('')
    setCandidatePassword('')
    setCandidatePortalError('')
    setOutcomePopup(null)
  }

  async function fetchCandidatePortal(email: string, password: string) {
    const previousStatus = candidatePortal?.profile.hiringStatus
    const response = await fetch('/api/auth/candidate-login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    })
    if (!response.ok) {
      const error = await response.json().catch(() => null) as { detail?: string; message?: string } | null
      throw new Error(error?.detail ?? error?.message ?? 'Could not load your profile. Check your email and password.')
    }
    const portal = await response.json() as CandidatePortal
    setCandidatePortal(portal)
    setOutcomePopup(
      (portal.profile.hiringStatus === 'REJECTED' || portal.profile.hiringStatus === 'OFFER_RELEASED')
        && portal.profile.hiringStatus !== previousStatus
        ? portal.profile.hiringStatus
        : null,
    )
  }

  async function refreshCandidatePortal() {
    setCandidatePortalError('')
    setIsRefreshingCandidatePortal(true)
    try {
      await fetchCandidatePortal(candidateEmail, candidatePassword)
    } catch (error) {
      setCandidatePortalError(error instanceof Error ? error.message : 'Unable to refresh your profile.')
    } finally {
      setIsRefreshingCandidatePortal(false)
    }
  }

  async function loginCandidate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setCandidatePortalError('')
    try {
      await fetchCandidatePortal(candidateEmail, candidatePassword)
    } catch (error) {
      setCandidatePortalError(error instanceof Error ? error.message : 'Unable to reach the server.')
    }
  }

  async function viewNewCandidateProfile() {
    openCandidatePortal()
    try {
      await fetchCandidatePortal(candidateEmail, candidatePassword)
    } catch (error) {
      setCandidatePortalError(error instanceof Error ? error.message : 'Unable to load your profile.')
    }
  }

  async function loadHrCandidates(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await refreshHrCandidates()
  }

  async function refreshHrCandidates() {
    setHrError('')
    setIsRefreshingHrCandidates(true)
    try {
      const response = await fetch('/api/hr/candidates', {
        headers: {
          Authorization: `Basic ${btoa(`${hrUsername}:${hrPassword}`)}`,
        },
      })
      if (!response.ok) {
        throw new Error(response.status === 401 || response.status === 403
          ? 'HR sign-in failed. Check your username and password.'
          : 'Could not load candidate registrations.')
      }
      const refreshedCandidates = await response.json() as RegisteredCandidate[]
      setCandidates(refreshedCandidates)
      setSelectedCandidate((current) => current
        ? refreshedCandidates.find((candidate) => candidate.id === current.id) ?? null
        : null)
    } catch (error) {
      setHrError(error instanceof Error ? error.message : 'Unable to reach the server.')
    } finally {
      setIsRefreshingHrCandidates(false)
    }
  }

  function refreshCurrentPage() {
    if (showCandidatePortal && candidatePortal) {
      void refreshCandidatePortal()
      return
    }
    if (showHrDashboard && candidates) {
      void refreshHrCandidates()
      return
    }
    window.location.reload()
  }

  async function changeCandidateStatus(status: HiringStatus) {
    if (!selectedCandidate) return
    setHrError('')
    try {
      const response = await fetch(`/api/hr/candidates/${selectedCandidate.id}/status`, {
        method: 'POST',
        headers: {
          Authorization: `Basic ${btoa(`${hrUsername}:${hrPassword}`)}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ status, notes: hrNotes, managerFeedback }),
      })
      if (!response.ok) {
        const error = await response.json().catch(() => null) as { detail?: string; message?: string } | null
        throw new Error(error?.detail ?? error?.message ?? 'Could not update the candidate stage.')
      }
      const updatedCandidate = await response.json() as RegisteredCandidate
      setCandidates((current) => current?.map((candidate) =>
        candidate.id === updatedCandidate.id ? updatedCandidate : candidate) ?? current)
      setSelectedCandidate(updatedCandidate)
      setHrNotes('')
      setManagerFeedback('')
    } catch (error) {
      setHrError(error instanceof Error ? error.message : 'Unable to reach the server.')
    }
  }

  async function deleteCandidate(candidate: RegisteredCandidate) {
    if (!window.confirm(
      `Are you sure you want to permanently delete ${candidate.firstName} ${candidate.lastName}? Their profile, application data, and resume will be deleted. Their phone number will also be blocked from registering again on this demo server. Only continue if this account is being removed for abusive language or misuse. This demo's block list may be cleared if the server restarts.`,
    )) {
      return
    }

    setHrError('')
    setDeletingCandidateId(candidate.id)
    try {
      const response = await fetch(`/api/hr/candidates/${candidate.id}`, {
        method: 'DELETE',
        headers: {
          Authorization: `Basic ${btoa(`${hrUsername}:${hrPassword}`)}`,
        },
      })
      if (!response.ok) {
        throw new Error(response.status === 401 || response.status === 403
          ? 'HR sign-in failed. Please sign in again.'
          : 'Could not delete this candidate.')
      }
      setCandidates((current) => current?.filter((item) => item.id !== candidate.id) ?? current)
      setSelectedCandidate((current) => current?.id === candidate.id ? null : current)
    } catch (error) {
      setHrError(error instanceof Error ? error.message : 'Unable to reach the server.')
    } finally {
      setDeletingCandidateId(null)
    }
  }

  async function advanceCandidateStatus() {
    if (!selectedCandidate) return
    const nextStatus = getNextHiringStatus(selectedCandidate.hiringStatus)
    if (nextStatus) await changeCandidateStatus(nextStatus)
  }

  async function downloadResume(candidate: RegisteredCandidate) {
    if (!candidate.resumeId) return
    setHrError('')
    try {
      const response = await fetch(`/api/hr/candidates/resumes/${candidate.resumeId}`, {
        headers: {
          Authorization: `Basic ${btoa(`${hrUsername}:${hrPassword}`)}`,
        },
      })
      if (!response.ok) throw new Error('Could not download this resume.')
      const fileUrl = URL.createObjectURL(await response.blob())
      setResumePreview({ url: fileUrl, fileName: candidate.resumeFileName || 'Candidate resume' })
    } catch (error) {
      setHrError(error instanceof Error ? error.message : 'Unable to download resume.')
    }
  }

  function closeResumePreview() {
    if (resumePreview) URL.revokeObjectURL(resumePreview.url)
    setResumePreview(null)
  }

  async function register(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setRegistrationError('')
    const formData = new FormData(event.currentTarget)
    const password = String(formData.get('password'))
    const confirmPassword = String(formData.get('confirmPassword'))
    const firstName = String(formData.get('firstName')).trim()
    const lastName = String(formData.get('lastName')).trim()
    const email = String(formData.get('email')).trim()
    const phone = String(formData.get('phone')).trim()
    const country = phoneCountries.find(({ code }) => code === formData.get('phoneCountry'))?.code

    if (password !== confirmPassword) {
      setRegistrationError('Passwords do not match.')
      return
    }

    const namePattern = /^[\p{L}][\p{L}\p{M} .'-]*$/u
    if (!namePattern.test(firstName) || !namePattern.test(lastName)) {
      setRegistrationError('First and last names must contain letters only (spaces, hyphens, apostrophes, and periods are allowed).')
      return
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email)) {
      setRegistrationError('Please enter a valid email address.')
      return
    }

    const currentDesignation = String(formData.get('currentDesignation')).trim()
    if (!/[\p{L}]/u.test(currentDesignation)) {
      setRegistrationError('Job title must include at least one letter. Numbers are allowed with a real title, for example Developer 2.')
      return
    }

    if (!country || !isValidPhoneNumber(phone, country)) {
      setRegistrationError('Please enter a valid phone number for the selected country.')
      return
    }

    const parsedPhone = parsePhoneNumber(phone, country)
    if (!parsedPhone?.isValid()) {
      setRegistrationError('Please enter a valid phone number for the selected country.')
      return
    }

    const resume = formData.get('resume')
    if (!(resume instanceof File) || resume.size === 0) {
      setRegistrationError('Please attach your resume.')
      return
    }
    const skills = String(formData.get('skills')).split(',').map((skill) => skill.trim()).filter(Boolean)
    if (skills.length === 0) {
      setRegistrationError('Please add at least one skill.')
      return
    }
    if (skills.some((skill) => !/[\p{L}]/u.test(skill))) {
      setRegistrationError('Each skill must include at least one letter. Numbers and versions are allowed, for example Java8 or SpringBoot3.8.')
      return
    }

    try {
      const registration = new FormData()
      registration.append('registration', new Blob([JSON.stringify({
        firstName,
        lastName,
        email,
        password,
        phone: parsedPhone.number,
        currentDesignation,
        experienceYears: Number(formData.get('experienceYears')),
        skills,
      })], { type: 'application/json' }))
      registration.append('resume', resume)
      const response = await fetch('/api/auth/register', {
        method: 'POST',
        body: registration,
      })

      if (!response.ok) {
        const error = await response.json().catch(() => null) as { message?: string; detail?: string; title?: string } | null
        throw new Error(error?.detail ?? error?.message ?? error?.title ?? 'Registration failed. Please try again.')
      }

      setCandidateEmail(email)
      setCandidatePassword(password)
      setRegistrationComplete(true)
    } catch (error) {
      setRegistrationError(error instanceof Error ? error.message : 'Unable to reach the server. Please try again.')
    }
  }

  if (showRegistration) {
    return (
      <main className="min-h-screen bg-slate-950 px-6 py-10 text-slate-100">
        <div className="mx-auto max-w-5xl">
          <div className="mb-8 flex items-center justify-between gap-4">
            <button
              type="button"
              onClick={closeRegistration}
              className="inline-flex items-center gap-2 text-sm text-slate-300 transition hover:text-white"
            >
              <ArrowLeft className="h-4 w-4" /> Back to home
            </button>
            <RefreshButton onClick={refreshCurrentPage} />
          </div>
          <div className="grid overflow-hidden rounded-3xl border border-slate-800 bg-slate-900 shadow-2xl md:grid-cols-[0.85fr_1.15fr]">
            <section className="hidden flex-col justify-between bg-indigo-600 p-10 md:flex">
              <div className="flex items-center gap-3">
                <Sparkles className="h-6 w-6" />
                <span className="text-sm font-semibold uppercase tracking-[0.2em]">TalentFlow AI</span>
              </div>
              <div>
                <h1 className="text-4xl font-bold leading-tight">Your next opportunity starts here.</h1>
                <p className="mt-4 text-indigo-100">Create a candidate account to manage your profile and applications.</p>
              </div>
              <p className="text-sm text-indigo-200">A clearer path from application to opportunity.</p>
            </section>

            <section className="p-6 sm:p-10">
              {registrationComplete ? (
                <div className="py-12 text-center" role="status">
                  <CheckCircle2 className="mx-auto h-12 w-12 text-emerald-400" />
                  <h2 className="mt-5 text-2xl font-semibold text-white">Account created</h2>
                  <p className="mt-2 text-slate-300">Your profile is submitted and waiting for HR review.</p>
                  <button
                    type="button"
                    onClick={() => void viewNewCandidateProfile()}
                    className="mt-6 rounded-xl bg-indigo-500 px-5 py-3 font-medium text-white transition hover:bg-indigo-400"
                  >
                    View my application status
                  </button>
                </div>
              ) : (
                <>
                  <p className="text-sm font-medium text-indigo-300">CANDIDATE PORTAL</p>
                  <h2 className="mt-2 text-3xl font-bold text-white">Create your account</h2>
                  <p className="mt-2 text-sm text-slate-400">Add your skills and resume so HR can review your profile.</p>
                  <form className="mt-8 space-y-5" onSubmit={register}>
                    <div className="grid gap-5 sm:grid-cols-2">
                      <label className="block text-sm text-slate-300">
                        First name
                        <input required name="firstName" autoComplete="given-name" maxLength={80} pattern="[\p{L}][\p{L}\p{M} .'-]*" title="Use letters only; spaces, hyphens, apostrophes, and periods are allowed." className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none focus:border-indigo-400" />
                      </label>
                      <label className="block text-sm text-slate-300">
                        Last name
                        <input required name="lastName" autoComplete="family-name" maxLength={80} pattern="[\p{L}][\p{L}\p{M} .'-]*" title="Use letters only; spaces, hyphens, apostrophes, and periods are allowed." className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none focus:border-indigo-400" />
                      </label>
                    </div>
                    <label className="block text-sm text-slate-300">
                      Email address
                      <input required type="email" name="email" autoComplete="email" maxLength={254} title="Enter an email address such as name@example.com." className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none focus:border-indigo-400" />
                    </label>
                    <div className="block text-sm text-slate-300">
                      <label htmlFor="phone-country">Phone number</label>
                      <div className="mt-2 grid gap-3 sm:grid-cols-[minmax(0,0.8fr)_minmax(0,1.2fr)]">
                        <select id="phone-country" name="phoneCountry" defaultValue="IN" autoComplete="country" className="w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none focus:border-indigo-400">
                          {phoneCountries.map(({ code, name, callingCode }) => (
                            <option key={code} value={code}>{name} (+{callingCode})</option>
                          ))}
                        </select>
                        <input required type="tel" name="phone" autoComplete="tel-national" inputMode="tel" placeholder="98765 43210" maxLength={25} aria-describedby="phone-help" className="w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none focus:border-indigo-400" />
                      </div>
                      <p id="phone-help" className="mt-2 text-xs text-slate-500">Choose your country and enter a valid phone number. We’ll save it with the country code.</p>
                    </div>
                    <label className="block text-sm text-slate-300">
                      Current / target job title
                      <input required name="currentDesignation" maxLength={120} placeholder="e.g. Frontend Developer 2" aria-describedby="job-title-help" className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none focus:border-indigo-400" />
                      <span id="job-title-help" className="mt-2 block text-xs text-slate-500">Include a title with at least one letter; seniority/version numbers are allowed.</span>
                    </label>
                    <div className="grid gap-5 sm:grid-cols-2">
                      <label className="block text-sm text-slate-300">
                        Years of experience
                        <input required type="number" name="experienceYears" min="0" max="60" defaultValue="0" className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none focus:border-indigo-400" />
                      </label>
                      <label className="block text-sm text-slate-300">
                        Skills (comma-separated)
                        <input required name="skills" placeholder="React, Java8, SpringBoot3.8" aria-describedby="skills-help" className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none focus:border-indigo-400" />
                        <span id="skills-help" className="mt-2 block text-xs text-slate-500">Each skill needs at least one letter; version numbers are allowed (e.g. Java8).</span>
                      </label>
                    </div>
                    <label className="block text-sm text-slate-300">
                      CV / Resume (text-based PDF only, max 10 MB; offer letters are rejected)
                      <input required type="file" name="resume" accept=".pdf,application/pdf" className="mt-2 block w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-sm text-slate-300 file:mr-4 file:rounded-lg file:border-0 file:bg-indigo-500 file:px-4 file:py-2 file:font-medium file:text-white" />
                    </label>
                    <div className="grid gap-5 sm:grid-cols-2">
                      <label className="block text-sm text-slate-300">
                        Password
                        <input required type="password" name="password" minLength={8} autoComplete="new-password" className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none focus:border-indigo-400" />
                      </label>
                      <label className="block text-sm text-slate-300">
                        Confirm password
                        <input required type="password" name="confirmPassword" minLength={8} autoComplete="new-password" className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none focus:border-indigo-400" />
                      </label>
                    </div>
                    {registrationError && <p className="text-sm text-rose-400" role="alert">{registrationError}</p>}
                    <button type="submit" className="w-full rounded-xl bg-indigo-500 px-5 py-3 font-semibold text-white shadow-lg shadow-indigo-500/20 transition hover:bg-indigo-400">
                      Create account
                    </button>
                  </form>
                </>
              )}
            </section>
          </div>
        </div>
      </main>
    )
  }

  if (showCandidatePortal) {
    return (
      <main className="min-h-screen bg-slate-950 px-6 py-10 text-slate-100">
        <div className="mx-auto max-w-5xl">
          <div className="mb-8 flex flex-wrap items-center justify-between gap-4">
            <button type="button" onClick={closeCandidatePortal} className="inline-flex items-center gap-2 text-sm text-slate-300 hover:text-white">
              <ArrowLeft className="h-4 w-4" /> Back to home
            </button>
            <RefreshButton
              onClick={refreshCurrentPage}
              busy={isRefreshingCandidatePortal}
              label={candidatePortal ? 'Refresh application status' : 'Refresh page'}
            />
          </div>
          <div className="flex items-center gap-3 text-indigo-300">
            <BriefcaseBusiness className="h-6 w-6" />
            <span className="text-sm font-semibold uppercase tracking-[0.24em]">TalentFlow AI · Candidate portal</span>
          </div>
          {candidatePortal === null ? (
            <section className="mt-8 max-w-md rounded-2xl border border-slate-800 bg-slate-900 p-6">
              <h1 className="text-2xl font-bold text-white">View your application status</h1>
              <p className="mt-2 text-sm text-slate-400">Sign in with the email and password you used to register.</p>
              <form onSubmit={loginCandidate} className="mt-6 space-y-4">
                <label className="block text-sm text-slate-300">
                  Email address
                  <input required type="email" autoComplete="email" value={candidateEmail} onChange={(event) => setCandidateEmail(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none focus:border-indigo-400" />
                </label>
                <label className="block text-sm text-slate-300">
                  Password
                  <input required type="password" autoComplete="current-password" value={candidatePassword} onChange={(event) => setCandidatePassword(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none focus:border-indigo-400" />
                </label>
                {candidatePortalError && <p role="alert" className="text-sm text-rose-400">{candidatePortalError}</p>}
                <button type="submit" className="w-full rounded-xl bg-indigo-500 px-5 py-3 font-semibold text-white hover:bg-indigo-400">View my profile</button>
              </form>
            </section>
          ) : (
            <section className="mt-8 rounded-2xl border border-slate-800 bg-slate-900 p-6">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <p className="text-sm font-medium text-indigo-300">MY CANDIDATE PROFILE</p>
                  <h1 className="mt-2 text-2xl font-bold text-white">{candidatePortal.profile.firstName} {candidatePortal.profile.lastName}</h1>
                  <p className="mt-1 text-slate-300">{candidatePortal.profile.currentDesignation || 'Role not specified'} · {candidatePortal.profile.experienceYears ?? 0} years experience</p>
                  <p className="mt-1 text-sm text-slate-400">{candidatePortal.profile.email}</p>
                </div>
                <button type="button" onClick={closeCandidatePortal} className="text-sm text-slate-400 hover:text-white">Sign out</button>
              </div>
              {candidatePortalError && <p role="alert" className="mt-4 text-sm text-rose-400">{candidatePortalError}</p>}

              <div className="mt-8 rounded-xl border border-slate-800 bg-slate-950/60 p-5">
                <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
                  <h2 className="font-semibold text-white">Hiring progress</h2>
                  <span className={`rounded-full px-3 py-1 text-sm font-medium ${
                    candidatePortal.profile.hiringStatus === 'REJECTED'
                      ? 'bg-rose-500/15 text-rose-300'
                      : candidatePortal.profile.hiringStatus === 'IN_REVIEW'
                        ? 'bg-amber-500/15 text-amber-200'
                        : 'bg-emerald-500/15 text-emerald-300'
                  }`}>
                    {getStageLabel(candidatePortal.profile.hiringStatus)}
                  </span>
                </div>
                <HiringProgress status={candidatePortal.profile.hiringStatus} />
              </div>

              <div className="mt-8">
                <h2 className="font-semibold text-white">My skills</h2>
                <div className="mt-3 flex flex-wrap gap-2">
                  {candidatePortal.profile.skills.map((skill) => (
                    <span key={skill} className="rounded-full border border-indigo-500/30 bg-indigo-500/10 px-3 py-1 text-sm text-indigo-200">{skill}</span>
                  ))}
                </div>
              </div>

              <div className="mt-8">
                <h2 className="font-semibold text-white">Status updates</h2>
                {candidatePortal.statusHistory.length === 0 ? (
                  <p className="mt-3 text-sm text-slate-400">No status updates yet.</p>
                ) : (
                  <ol className="mt-4 space-y-4 border-l border-slate-700 pl-5">
                    {candidatePortal.statusHistory.map((entry, index) => (
                      <li key={`${entry.status}-${entry.changedAt}-${index}`} className="relative">
                        <span className={`absolute -left-[1.57rem] top-1 h-3 w-3 rounded-full ${
                          entry.status === 'REJECTED' ? 'bg-rose-400' : 'bg-emerald-400'
                        }`} />
                        <p className={`font-medium ${entry.status === 'REJECTED' ? 'text-rose-300' : 'text-white'}`}>{getStageLabel(entry.status)}</p>
                        <p className="mt-1 text-xs text-slate-500">{new Date(entry.changedAt).toLocaleString()}</p>
                      </li>
                    ))}
                  </ol>
                )}
              </div>
            </section>
          )}
          {candidatePortal && outcomePopup && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 p-4" role="presentation">
              <section
                role="dialog"
                aria-modal="true"
                aria-labelledby="application-outcome-title"
                className={`w-full max-w-md rounded-3xl border p-7 text-center shadow-2xl ${
                  outcomePopup === 'REJECTED'
                    ? 'border-rose-500/40 bg-slate-900 shadow-rose-950/30'
                    : 'border-emerald-500/40 bg-slate-900 shadow-emerald-950/30'
                }`}
              >
                <p className="text-5xl" aria-hidden="true">{outcomePopup === 'REJECTED' ? '💛' : '🎉😊'}</p>
                <h2 id="application-outcome-title" className={`mt-4 text-2xl font-bold ${
                  outcomePopup === 'REJECTED' ? 'text-rose-200' : 'text-emerald-200'
                }`}>
                  {outcomePopup === 'REJECTED' ? 'Keep believing in yourself' : 'Congratulations!'}
                </h2>
                <p className="mt-3 leading-6 text-slate-300">
                  {outcomePopup === 'REJECTED'
                    ? 'Every opportunity is a chance to learn and grow. This decision does not define your potential—keep moving forward.'
                    : 'Your offer has been released. Your hard work paid off, and we are excited to welcome you to the team!'}
                </p>
                <p className={`mt-4 font-semibold ${
                  outcomePopup === 'REJECTED' ? 'text-rose-300' : 'text-emerald-300'
                }`}>
                  {outcomePopup === 'REJECTED' ? 'Good luck for your next opportunity!' : 'Welcome aboard!'}
                </p>
                <button
                  type="button"
                  autoFocus
                  onClick={() => setOutcomePopup(null)}
                  className={`mt-7 rounded-xl px-5 py-3 font-semibold text-slate-950 ${
                    outcomePopup === 'REJECTED'
                      ? 'bg-rose-300 hover:bg-rose-200'
                      : 'bg-emerald-300 hover:bg-emerald-200'
                  }`}
                >
                  {outcomePopup === 'REJECTED' ? 'Continue' : 'Thank you!'}
                </button>
              </section>
            </div>
          )}
        </div>
      </main>
    )
  }

  if (showHrDashboard) {
    return (
      <main className="min-h-screen bg-slate-950 px-6 py-10 text-slate-100">
        <div className="mx-auto max-w-6xl">
          <div className="mb-8 flex items-center justify-between gap-4">
            <button type="button" onClick={closeHrDashboard} className="inline-flex items-center gap-2 text-sm text-slate-300 hover:text-white">
              <ArrowLeft className="h-4 w-4" /> Back to home
            </button>
            <RefreshButton
              onClick={refreshCurrentPage}
              busy={isRefreshingHrCandidates}
              label={candidates ? 'Refresh candidate list' : 'Refresh page'}
            />
          </div>
          <div className="flex items-center gap-3 text-indigo-300">
            <Users className="h-6 w-6" />
            <span className="text-sm font-semibold uppercase tracking-[0.24em]">TalentFlow AI · HR</span>
          </div>
          <h1 className="mt-4 text-3xl font-bold text-white md:text-4xl">Candidate registrations</h1>
          <p className="mt-2 text-slate-400">Sign in with your HR administrator account to view registered candidates.</p>

          {candidates === null ? (
            <form onSubmit={loadHrCandidates} className="mt-8 max-w-md space-y-4 rounded-2xl border border-slate-800 bg-slate-900 p-6">
              <label className="block text-sm text-slate-300">
                HR username
                <input required autoComplete="username" value={hrUsername} onChange={(event) => setHrUsername(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none focus:border-indigo-400" />
              </label>
              <label className="block text-sm text-slate-300">
                Password
                <input required type="password" autoComplete="current-password" value={hrPassword} onChange={(event) => setHrPassword(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none focus:border-indigo-400" />
              </label>
              {hrError && <p role="alert" className="text-sm text-rose-400">{hrError}</p>}
              <button type="submit" className="w-full rounded-xl bg-indigo-500 px-5 py-3 font-semibold text-white hover:bg-indigo-400">Sign in and view candidates</button>
            </form>
          ) : (
            <section className="mt-8 overflow-hidden rounded-2xl border border-slate-800 bg-slate-900">
              <div className="flex items-center justify-between border-b border-slate-800 px-6 py-4">
                <p className="font-medium text-white">{candidates.length} registered candidate{candidates.length === 1 ? '' : 's'}</p>
                <button type="button" onClick={() => { setCandidates(null); setHrUsername(''); setHrPassword(''); setSelectedCandidate(null) }} className="text-sm text-indigo-300 hover:text-indigo-200">Sign out</button>
              </div>
              {candidates.length === 0 ? (
                <p className="px-6 py-10 text-center text-slate-400">No candidate registrations yet.</p>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-slate-800/70 text-slate-300">
                      <tr>
                        <th className="px-6 py-3 font-medium">Candidate</th>
                        <th className="px-6 py-3 font-medium">Email</th>
                        <th className="px-6 py-3 font-medium">Skills</th>
                        <th className="px-6 py-3 font-medium">Hiring stage</th>
                        <th className="px-6 py-3 font-medium">Registered</th>
                        <th className="px-6 py-3 font-medium">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800">
                      {candidates.map((candidate) => (
                        <tr key={candidate.id} className="text-slate-200">
                          <td className="px-6 py-4 font-medium text-white">{candidate.firstName} {candidate.lastName}</td>
                          <td className="px-6 py-4">{candidate.email}</td>
                          <td className="px-6 py-4">{candidate.skills.slice(0, 3).join(', ') || '—'}</td>
                          <td className="px-6 py-4">
                            <span className={`rounded-full px-3 py-1 text-xs font-medium ${
                              candidate.hiringStatus === 'REJECTED' ? 'bg-rose-500/15 text-rose-300'
                                : candidate.hiringStatus === 'IN_REVIEW' ? 'bg-amber-500/15 text-amber-200'
                                  : 'bg-emerald-500/15 text-emerald-300'
                            }`}>{getStageLabel(candidate.hiringStatus)}</span>
                          </td>
                          <td className="px-6 py-4">{new Date(candidate.registeredAt).toLocaleString()}</td>
                          <td className="px-6 py-4">
                            <div className="flex items-center gap-4">
                              <button type="button" onClick={() => { setSelectedCandidate(candidate); setHrNotes(''); setManagerFeedback(candidate.managerFeedback ?? '') }} className="text-indigo-300 hover:text-indigo-200">Review profile</button>
                              <button
                                type="button"
                                aria-label={`Delete ${candidate.firstName} ${candidate.lastName}`}
                                disabled={deletingCandidateId !== null}
                                onClick={() => void deleteCandidate(candidate)}
                                className="inline-flex items-center gap-1.5 rounded-lg border border-rose-500/40 bg-rose-500/10 px-3 py-1.5 text-xs font-semibold text-rose-300 transition hover:border-rose-400 hover:bg-rose-500/20 hover:text-rose-200 disabled:cursor-not-allowed disabled:opacity-50"
                              >
                                <Trash2 className="h-3.5 w-3.5" />
                                {deletingCandidateId === candidate.id ? 'Deleting…' : 'Delete'}
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              {hrError && <p role="alert" className="border-t border-slate-800 px-6 py-3 text-sm text-rose-400">{hrError}</p>}
            </section>
          )}
          {selectedCandidate && (
            <section className="mt-6 rounded-2xl border border-indigo-500/30 bg-slate-900 p-6">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <p className="text-sm font-medium text-indigo-300">CANDIDATE PROFILE</p>
                  <h2 className="mt-2 text-2xl font-semibold text-white">{selectedCandidate.firstName} {selectedCandidate.lastName}</h2>
                  <p className="mt-1 text-slate-300">{selectedCandidate.currentDesignation || 'Role not specified'} · {selectedCandidate.experienceYears ?? 0} years experience</p>
                  <p className="mt-1 text-sm text-slate-400">{selectedCandidate.email} · {selectedCandidate.phone || 'No phone provided'}</p>
                </div>
                <button type="button" onClick={() => setSelectedCandidate(null)} className="text-sm text-slate-400 hover:text-white">Close</button>
              </div>
              <div className="mt-6 rounded-xl border border-slate-800 bg-slate-950/60 p-5">
                <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
                  <h3 className="font-semibold text-white">Hiring pipeline</h3>
                  <span className="rounded-full bg-amber-500/15 px-3 py-1 text-sm font-medium text-amber-200">{getStageLabel(selectedCandidate.hiringStatus)}</span>
                </div>
                <HiringProgress status={selectedCandidate.hiringStatus} />
                {selectedCandidate.hiringStatus !== 'REJECTED' && selectedCandidate.hiringStatus !== 'OFFER_RELEASED' && (
                  <div className="mt-6 space-y-4 border-t border-slate-800 pt-5">
                    <label className="block text-sm text-slate-300">
                      HR notes for this update
                      <textarea rows={2} maxLength={1500} value={hrNotes} onChange={(event) => setHrNotes(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-900 px-4 py-3 text-white outline-none focus:border-indigo-400" placeholder="Optional update or interview details" />
                    </label>
                    {selectedCandidate.hiringStatus === 'SELECTED_FOR_MANAGER_ROUND' && (
                      <label className="block text-sm text-slate-300">
                        Manager / lead feedback (required to clear this round)
                        <textarea rows={3} maxLength={1500} value={managerFeedback} onChange={(event) => setManagerFeedback(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-900 px-4 py-3 text-white outline-none focus:border-indigo-400" placeholder="Enter the manager's interview feedback" />
                      </label>
                    )}
                    <div className="flex flex-wrap gap-3">
                      {getNextHiringStatus(selectedCandidate.hiringStatus) && (
                        <button
                          type="button"
                          disabled={selectedCandidate.hiringStatus === 'SELECTED_FOR_MANAGER_ROUND' && !managerFeedback.trim()}
                          onClick={() => void advanceCandidateStatus()}
                          className="rounded-xl bg-indigo-500 px-4 py-2 font-medium text-white hover:bg-indigo-400 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          Move to {getStageLabel(getNextHiringStatus(selectedCandidate.hiringStatus) ?? selectedCandidate.hiringStatus)}
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => void changeCandidateStatus('REJECTED')}
                        className="rounded-xl border border-rose-500/40 px-4 py-2 font-medium text-rose-300 hover:bg-rose-500/10"
                      >
                        Reject candidate
                      </button>
                    </div>
                  </div>
                )}
                {selectedCandidate.managerFeedback && selectedCandidate.hiringStatus !== 'SELECTED_FOR_MANAGER_ROUND' && (
                  <div className="mt-5 border-t border-slate-800 pt-4">
                    <h4 className="text-sm font-medium text-slate-300">Manager feedback</h4>
                    <p className="mt-2 whitespace-pre-wrap text-sm text-slate-400">{selectedCandidate.managerFeedback}</p>
                  </div>
                )}
              </div>
              <h3 className="mt-6 text-sm font-medium uppercase tracking-wide text-slate-400">Skills</h3>
              <div className="mt-3 flex flex-wrap gap-2">
                {selectedCandidate.skills.map((skill) => (
                  <span key={skill} className="rounded-full border border-indigo-500/30 bg-indigo-500/10 px-3 py-1 text-sm text-indigo-200">{skill}</span>
                ))}
              </div>
              <h3 className="mt-6 text-sm font-medium uppercase tracking-wide text-slate-400">Skills detected in CV</h3>
              <div className="mt-3 flex flex-wrap gap-2">
                {selectedCandidate.cvSkills.length > 0
                  ? selectedCandidate.cvSkills.map((skill) => (
                    <span key={skill} className="rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-1 text-sm text-emerald-200">{skill}</span>
                  ))
                  : <span className="text-sm text-slate-400">No supported skills were detected in the document text.</span>}
              </div>
              <p className="mt-3 text-sm text-slate-300">
                CV-stated experience: {selectedCandidate.cvExperienceYears === null ? 'Not detected' : `${selectedCandidate.cvExperienceYears} years`}
                {selectedCandidate.resumeScreeningScore !== null && ` · CV screening score (rule-based): ${selectedCandidate.resumeScreeningScore}%`}
              </p>
              {selectedCandidate.resumeScreeningSummary && (
                <p className="mt-2 text-sm text-slate-400">{selectedCandidate.resumeScreeningSummary}</p>
              )}
              <h3 className="mt-6 text-sm font-medium uppercase tracking-wide text-slate-400">Job matches based on CV skills</h3>
              {selectedCandidate.jobMatches.length > 0 ? (
                <div className="mt-3 space-y-3">
                  {selectedCandidate.jobMatches.map((match) => (
                    <article key={match.jobId} className="rounded-xl border border-slate-800 bg-slate-950/60 p-4">
                      <div className="flex items-center justify-between gap-3">
                        <h4 className="font-medium text-white">{match.title}</h4>
                        <span className="rounded-full bg-indigo-500/15 px-3 py-1 text-sm font-semibold text-indigo-200">{match.matchScore}% skill match</span>
                      </div>
                      <p className="mt-2 text-sm text-slate-400">Matched: {match.matchedSkills.join(', ') || 'None'}</p>
                      {match.missingSkills.length > 0 && <p className="mt-1 text-sm text-slate-400">Missing: {match.missingSkills.join(', ')}</p>}
                    </article>
                  ))}
                </div>
              ) : <p className="mt-3 text-sm text-slate-400">No active jobs with skill requirements to compare against yet.</p>}
              <div className="mt-6 border-t border-slate-800 pt-5">
                <h3 className="text-sm font-medium uppercase tracking-wide text-slate-400">Resume</h3>
                {selectedCandidate.resumeId ? (
                  <button type="button" onClick={() => void downloadResume(selectedCandidate)} className="mt-3 inline-flex rounded-xl bg-indigo-500 px-4 py-2 font-medium text-white hover:bg-indigo-400">
                    View resume · {selectedCandidate.resumeFileName}
                  </button>
                ) : <p className="mt-3 text-sm text-slate-400">No resume was attached.</p>}
              </div>
            </section>
          )}
          {resumePreview && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/90 p-4 sm:p-8" role="dialog" aria-modal="true" aria-label="Resume preview">
              <section className="flex h-[90vh] w-full max-w-6xl flex-col overflow-hidden rounded-2xl border border-slate-700 bg-slate-900">
                <header className="flex items-center justify-between gap-4 border-b border-slate-700 px-5 py-3">
                  <h2 className="truncate font-medium text-white">{resumePreview.fileName}</h2>
                  <div className="flex shrink-0 items-center gap-4">
                    <a href={resumePreview.url} download={resumePreview.fileName} className="text-sm text-indigo-300 hover:text-indigo-200">Download</a>
                    <button type="button" onClick={closeResumePreview} className="text-sm text-slate-300 hover:text-white">Close</button>
                  </div>
                </header>
                <iframe title={`Resume: ${resumePreview.fileName}`} src={resumePreview.url} className="min-h-0 flex-1 bg-white" />
              </section>
            </div>
          )}
        </div>
      </main>
    )
  }

  return (
    <main className="min-h-screen bg-slate-950 text-slate-100">
      <div className="mx-auto max-w-7xl px-6 py-16">
        <div className="mb-10 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3 text-indigo-300">
            <Sparkles className="h-6 w-6" />
            <span className="text-sm font-semibold uppercase tracking-[0.28em]">TalentFlow AI</span>
          </div>
          <RefreshButton onClick={refreshCurrentPage} />
        </div>

        <section className="grid gap-10 lg:grid-cols-[1.2fr_0.8fr] lg:items-center">
          <div>
            <p className="mb-4 inline-flex rounded-full border border-indigo-500/40 bg-indigo-500/10 px-3 py-1 text-xs font-medium text-indigo-200">
              Enterprise recruitment platform
            </p>
            <h1 className="max-w-xl text-4xl font-bold tracking-tight text-white md:text-6xl">
              Recruitment intelligence for modern HR teams.
            </h1>
            <p className="mt-6 max-w-xl text-lg text-slate-300">
              Candidate lifecycle management, secure hiring workflows, and controlled AI-driven insights in one scalable platform.
            </p>
            <div className="mt-8 flex flex-wrap gap-4">
              <button onClick={openHrDashboard} className="rounded-xl bg-indigo-500 px-5 py-3 font-medium text-white shadow-lg shadow-indigo-500/20 transition hover:bg-indigo-400">
                HR dashboard
              </button>
              <button onClick={openRegistration} className="rounded-xl border border-slate-700 bg-slate-900 px-5 py-3 font-medium text-slate-200 transition hover:border-slate-500 hover:bg-slate-800">
                Register as candidate
              </button>
              <button onClick={openCandidatePortal} className="rounded-xl border border-indigo-500/40 bg-indigo-500/10 px-5 py-3 font-medium text-indigo-200 transition hover:bg-indigo-500/20">
                View my profile
              </button>
            </div>
          </div>

          <div className="rounded-3xl border border-slate-800 bg-slate-900/80 p-6 shadow-2xl shadow-slate-950/40">
            <div className="grid gap-4">
              <div className="rounded-2xl bg-slate-800 p-4">
                <p className="text-sm text-slate-400">Candidates registered</p>
                <div className="mt-3 flex items-end justify-between">
                  <span className="text-3xl font-bold text-white">12,480</span>
                  <span className="text-sm font-medium text-emerald-400">+18.4%</span>
                </div>
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="rounded-2xl bg-slate-800 p-4">
                  <p className="text-sm text-slate-400">Pending HR review</p>
                  <p className="mt-2 text-2xl font-semibold text-white">248</p>
                </div>
                <div className="rounded-2xl bg-slate-800 p-4">
                  <p className="text-sm text-slate-400">AI match score</p>
                  <p className="mt-2 text-2xl font-semibold text-white">89.4</p>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="mt-16 grid gap-6 md:grid-cols-3">
          {highlights.map(({ icon: Icon, title, text }) => (
            <div key={title} className="rounded-2xl border border-slate-800 bg-slate-900 p-6">
              <div className="mb-4 inline-flex rounded-xl bg-indigo-500/10 p-3 text-indigo-300">
                <Icon className="h-5 w-5" />
              </div>
              <h2 className="text-xl font-semibold text-white">{title}</h2>
              <p className="mt-3 text-sm leading-6 text-slate-300">{text}</p>
            </div>
          ))}
        </section>
      </div>
    </main>
  )
}
